(ns elb-log-tool.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [clj-time.coerce :as time.coerce]
            [amazonica.aws.ec2 :as ec2])
  (:import [org.joda.time DateTime]))

(def ^:private date-and-string-coercions (merge {DateTime (fn [x] (time.coerce/from-string x))}
                                                sc/+string-coercions+))

(def region {:endpoint (apply s/enum (->> (ec2/describe-regions) :regions (map :region-name)))})

(def log-query {:load-balancer-name s/Str
                (s/optional-key :year) s/Int
                (s/optional-key :month) s/Int
                (s/optional-key :day) s/Int})

(def log-entry
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

(def log-entry-coercer (sc/coercer log-entry date-and-string-coercions))


