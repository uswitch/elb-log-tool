(ns elb-log-tool.replay
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [cemerick.url]))

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
  (let [u         (cemerick.url/url "http://mobiles-deals-api.uswitchinternal.com:80/v3/publications/uswitch/deals?limit=10&offset=0&order=bestseller&profile-key=samsung&q=%5B%22and%22%2C%5B%22contains%22%2C%7B%22product%22%3A%7B%22manufacturer%22%3A%22samsung%22%7D%7D%5D%5D")
        overrides {:host "localhost"
                   :port 7020}]
    (->> overrides
         (into [])
         flatten
         (apply assoc u)))

  (log-entry->http-request {:host "localhost"
                            :port 7020}
                           30000
                           {:backend-ip "172.22.106.103"
                            :backend-port 7020
                            :backend-processing-time-seconds 0.002993
                            :backend-status-code 200
                            :client-ip "172.22.107.26"
                            :client-port 31796
                            :elb-name "mobilesdealsapi-v2"
                            :elb-status-code 200
                            :received-bytes 0
                            :request-method "GET"
                            :request-processing-time-seconds 3.7E-5
                            :request-protocol "HTTP/1.1"
                            :request-url "http://mobiles-deals-api.uswitchinternal.com:80/v3/publications/uswitch/deals?limit=10&offset=0&order=bestseller&profile-key=samsung&q=%5B%22and%22%2C%5B%22contains%22%2C%7B%22product%22%3A%7B%22manufacturer%22%3A%22samsung%22%7D%7D%5D%5D"
                            :response-processing-time-seconds 2.1E-5
                            :sent-bytes 16641
                            :ssl-cipher "-"
                            :ssl-protocol "-"
                            :timestamp (clj-time.core/date-time 2016 4 19 23 0 9)
                            :user-agent "Faraday v0.9.2"}))

(def run-state (atom {:request-channel  (async/chan 1000)
                      :response-channel (async/chan (async/dropping-buffer 20))
                      :sleep-delay      nil}))

(defn set-rate! [queries-per-second]
  (log/infof "Setting rate to %d queries per second." queries-per-second)
  (swap! run-state assoc :sleep-delay (int (/ 1000 queries-per-second))))

(defn start-queries
  ([queries-per-second log-seq]
   (async/go (async/onto-chan (:request-channel @run-state) log-seq))
   (start-queries queries-per-second))
  ([queries-per-second]
   (set-rate! queries-per-second)
   (async/go-loop [request (async/<! (:request-channel @run-state))]
                  (if (and request (:sleep-delay @run-state))
                    (do
                      (log/debugf "sending request %s" (pr-str request))
                      (async/>! (:response-channel @run-state) (http/request request))
                      (async/<! (async/timeout (:sleep-delay @run-state)))
                      (recur (async/<! (:request-channel @run-state))))
                    (log/info "query loop shutting down.")))))

(defn stop []
  (log/info "stopping the query loop.")
  (swap! run-state assoc :sleep-delay nil))

(comment
  (require '[elb-log-tool.log-stream :as log-stream])
  (require '[schema.core :as s])
  (def test-log-seq (s/with-fn-validation
                      (->> (log-stream/log-seq {:endpoint "eu-west-1"} {:load-balancer-name "mobilesdealsapi-v2" :year 2016 :month 4 :day 21})
                           (filter :timestamp)  ;; ignore rows without a timestamp
                           )))

  (start-queries 2 b)
  (set-rate! 10)
  (stop))

