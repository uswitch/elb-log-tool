(ns elb-log-tool.log-stream
  (:require [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [schema.core :as s]
            [schema.coerce :as sc]
            [clj-time.coerce :as time.coerce]
            [amazonica.aws.identitymanagement :as iam]
            [amazonica.aws.elasticloadbalancing :as elb]
            [amazonica.aws.s3 :as s3]
            [amazonica.aws.ec2 :as ec2])
  (:import [org.joda.time DateTime]))

(defn- elb-log-bucket
  [region log-query]
  (->> (select-keys log-query [:load-balancer-name])
       (elb/describe-load-balancer-attributes region)
       :load-balancer-attributes
       :access-log))

(defn- get-account-id []
  (-> (iam/get-user)
      :user
      :arn
      (clojure.string/split #":")
      (get 4)))

(defn- elb-log-files
  [region log-query]
  (log/debugf "listing log files on S3 matching query %s" (pr-str log-query))
  (let [access-log-attributes  (elb-log-bucket region log-query)
        _                      (when-not (:enabled access-log-attributes)
                                 (throw (ex-info (str "ELB " (:load-balancer-name log-query) " does not log to an S3 bucket.")
                                                 {:load-balancer-name    (:load-balancer-name log-query)
                                                  :access-log-attributes access-log-attributes})))
        date-components        (->> [(:year log-query) (:month log-query) (:day log-query)]
                                    (take-while identity)
                                    (map #(format "%02d" %)))
        prefix-components      (concat [(:s3bucket-prefix access-log-attributes) "AWSLogs" (get-account-id) "elasticloadbalancing" (:endpoint region)] date-components)
        location               {:bucket-name (:s3bucket-name access-log-attributes)
                                :prefix      (clojure.string/join "/" prefix-components)}]
    (loop [object-summaries []
           next-marker      nil]
      (log/debugf "querying S3 from %s" (if next-marker next-marker "the start of file listing."))
      (let [query    (merge location next-marker)
            response (s3/list-objects query)
            new-os   (concat object-summaries (:object-summaries response))]
        (if (:truncated? response)
          (recur new-os {:marker (:next-marker response)})
          (sort-by :key new-os))))))

(def ^:private raw-log-headings [:timestamp :elb-name :client-port :backend-port :request-processing-time-seconds :backend-processing-time-seconds
                                 :response-processing-time-seconds :elb-status-code :backend-status-code :received-bytes :sent-bytes :request :user-agent
                                 :ssl-cipher :ssl-protocol])

(def ^:private date-and-string-coercions (merge {DateTime (fn [x] (time.coerce/from-string x))}
                                                sc/+string-coercions+))

(def region-schema {:endpoint (apply s/enum (->> (ec2/describe-regions) :regions (map :region-name)))})

(def log-query-schema {:load-balancer-name s/Str
                       (s/optional-key :year) s/Int
                       (s/optional-key :month) s/Int
                       (s/optional-key :day) s/Int})

(def log-entry-schema
  {:timestamp                        DateTime
   :elb-name                         s/Str
   :client-ip                        s/Str
   :client-port                      s/Int
   :backend-ip                       s/Str
   :backend-port                     s/Int
   :request-processing-time-seconds  s/Num
   :backend-processing-time-seconds  s/Num
   :response-processing-time-seconds s/Num
   :elb-status-code                  s/Int
   :backend-status-code              s/Int
   :received-bytes                   s/Int
   :sent-bytes                       s/Int
   :request-method                   s/Str
   :request-url                      s/Str
   :request-protocol                 s/Str
   :user-agent                       s/Str
   :ssl-cipher                       s/Str
   :ssl-protocol                     s/Str})

(def ^:private log-entry-coercer (sc/coercer log-entry-schema date-and-string-coercions))

(defn- csv-line->log-entry
  [csv-line-vec]
  (let [m (zipmap raw-log-headings csv-line-vec)
        [client-ip client-port] (clojure.string/split (:client-port m) #":")
        [backend-ip backend-port] (clojure.string/split (:backend-port m) #":")
        [request-method request-url request-protocol] (clojure.string/split (:request m) #" ")]
    (-> m
        (assoc :client-ip        client-ip
               :client-port      client-port
               :backend-ip       backend-ip
               :backend-port     backend-port
               :request-method   request-method
               :request-url      request-url
               :request-protocol request-protocol)
        (dissoc :request)
        log-entry-coercer)))

(defn- log-entries
  [log-file]
  (log/debugf "reading log entries from S3 object %s" (:key log-file))
  (with-open [rdr (some-> (select-keys log-file [:bucket-name :key])
                          s3/get-object
                          :input-stream
                          io/reader)]
    (doall
      (->> (csv/read-csv rdr :separator \space)
           (map csv-line->log-entry)))))

(defn- merge-group-log-entries
  [files]
  (log/debugf "combining log entries from %d files in log group." (count files))
  (->> (map log-entries files)
       flatten
       (sort-by :timestamp)))

(s/defn log-seq :- log-entry-schema
  "Read the logs of an elastic load balancer from S3, with the logs in time order.
   Where there are log entries from multiple availability zones in the region, open
   the log files for that time index as a group and return their logs in time sequence."
  [region :- region-schema {:keys [load-balancer-name] :as log-query} :- log-query-schema]
  (when-let [files (elb-log-files region log-query)]
    (let [ts-index            (+ (-> files first :key (.indexOf load-balancer-name)) (count load-balancer-name) 1)
          ts-end              (+ ts-index 14)
          timestamp-from-name (fn [log-file] (-> log-file :key (subs ts-index ts-end)))]
      (->> files
           (partition-by timestamp-from-name)
           (map merge-group-log-entries)
           flatten))))

(comment
  (let [region       {:endpoint "eu-west-1"}
        log-query    {:load-balancer-name "mobilesdealsapi-v2" :year 2016 :month 4 :day 20}]
    ; (elb-log-bucket region log-query)
    ; (def test-files (elb-log-files region log-query))
    (def test-log-entries (log-seq region log-query))
    )

  (->> test-log-entries
       (drop 1000)
       (take 20)
       (map :timestamp)
       clojure.pprint/pprint)

  )

