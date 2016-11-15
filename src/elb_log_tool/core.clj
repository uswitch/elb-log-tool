(ns elb-log-tool.core
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [schema.core :as s]
            [cemerick.url]
            [elb-log-tool.replay :as replay]
            [elb-log-tool.db-writer :as db-writer]
            [elb-log-tool.log-stream :as log-stream]))

;;;;;;;;;;;;;;;; Example for replaying logs to an HTTP endpoint.

(defn override-url-elements
  [url-overrides url]
  (if (seq url-overrides)
    (->> url-overrides
         (into [])
         flatten
         (apply assoc url))
    url))

(defn log-entry->http-request
  [url-overrides request-timeout log-entry]
  (with-meta {:method  (-> log-entry :request-method clojure.string/lower-case keyword)
              :url     (->> (cemerick.url/url (:request-url log-entry))
                            (override-url-elements url-overrides)
                            str)
              :timeout request-timeout}
             {:log-entry log-entry}))

(comment
  ; The map of override rules given to log-entry->http-request can be empty if you simply wish to replay to the same endpoint.
  (def replay-run-state (atom {:request-channel  (async/chan (async/buffer 1000)
                                                             (map (partial log-entry->http-request {:host "localhost" :port 7020} 30000)))
                               :response-channel (async/chan (async/dropping-buffer 20))  ;; discard responses
                               :sleep-delay      nil}))

  ; wrap with (s/with-fn-validation) if you want the training wheels on.  Best have lots of RAM since
  ; the log-seq must be fully evaluated in order to pass the schema check.

  ; start replaying logs at 2 queries per second
  (->> (log-stream/log-seq {:endpoint "eu-west-1"} {:load-balancer-name "mobilesdealsapi-v2" :year 2016 :month 4 :day 21})
       (filter :timestamp)  ;; ignore rows without a timestamp
       (replay/start! replay-run-state 2))

  (replay/set-rate! replay-run-state 10)
  (replay/stop! replay-run-state)
  (replay/start! replay-run-state 2))

;;;;;;;;;;;;;;;; Example of writing the logs to a postgres table.

(defn user-name
  []
  (-> (System/getProperty "user.name")
      (clojure.string/lower-case)
      (clojure.string/replace #"\W" "-")))

(comment
  (def local-db {:classname   "org.postgresql.Driver"
                 :subprotocol "postgresql"
                 :subname     (str "//127.0.0.1/" (user-name))})

  (db-writer/create-table local-db)
  
  ; wrap with (s/with-fn-validation) if you want the training wheels on.  Best have lots of RAM since
  ; the log-seq must be fully evaluated in order to pass the schema check.
  (->> (log-stream/log-seq {:endpoint "eu-west-1"} {:load-balancer-name "mobilesdealsapi-20160617" :year 2016 :month 11 :day 15})
       (filter :timestamp)  ;; ignore rows without a timestamp
       (db-writer/write-to-table local-db)))


