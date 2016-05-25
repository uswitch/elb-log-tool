(ns elb-log-tool.replay
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [schema.core :as s]
            [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [elb-log-tool.schema :as schema]))


(s/defn set-rate! [run-state          :- schema/replay-run-state
                   queries-per-second :- s/Int] :- clojure.lang.Atom
  (log/infof "Setting rate to %d queries per second." queries-per-second)
  (swap! run-state assoc :sleep-delay (if (= queries-per-second 0)
                                        nil
                                        (int (/ 1000 queries-per-second)))))

(s/defn start!
  ([run-state :- schema/replay-run-state
    queries-per-second :- s/Int
    log-seq :- [schema/log-entry]]
   (async/go (async/onto-chan (:request-channel @run-state) log-seq))
   (start! run-state queries-per-second))
  ([run-state :- schema/replay-run-state
    queries-per-second :- s/Int]
   (set-rate! run-state queries-per-second)
   (async/go-loop [request (async/<! (:request-channel @run-state))]
                  (if (and request (:sleep-delay @run-state))
                    (do
                      (log/debugf "sending request %s" (pr-str request))
                      (async/>! (:response-channel @run-state) (http/request request))
                      (async/<! (async/timeout (:sleep-delay @run-state)))
                      (recur (async/<! (:request-channel @run-state))))
                    (log/info "query loop shutting down.")))))

(s/defn stop! [run-state :- schema/replay-run-state] :- clojure.lang.Atom
  (log/info "stopping the query loop.")
  (swap! run-state assoc :sleep-delay nil))

