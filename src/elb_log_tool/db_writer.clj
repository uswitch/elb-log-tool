(ns elb-log-tool.db-writer
  (:require [clojure.java.jdbc :as sql]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [clj-time.coerce :as time.coerce]
            [camel-snake-kebab.core :as csk]
            [elb-log-tool.schema :as schema])
  (:import [org.joda.time DateTime]))

(extend-protocol sql/ISQLValue
  DateTime
  (sql-value [value] (time.coerce/to-sql-time value)))

(def ^:private schema->postgres-column
  {DateTime     "timestamp not null"
   s/Str        "text"
   s/Num        "numeric(13,8)"
   s/Int        "int"})

(def ^:private table-spec (reduce (fn [result [k v]] (assoc result k (schema->postgres-column v)))
                                  {}
                                  schema/log-entry))

(def ^:private key-list (keys table-spec))
(def ^:private get-row-values-in-key-order (apply juxt key-list))



(defn- clojure-keyword->postgres-entity
  [entity]
  (->> entity
       csk/->snake_case_string
       (format "\"%s\"")))

(defn create-table
  [db]
  (->> (sql/create-table-ddl :elb-logs table-spec
                             {:entities clojure-keyword->postgres-entity})
       (sql/db-do-commands db)))

(defn write-to-table
  [db elb-name log-seq]
  (doseq [batch (partition-all 10000 log-seq)]
    (log/debugf "Inserting a batch of %d rows." (count batch))
    (let [rows (map get-row-values-in-key-order batch)]
      (sql/insert-multi! db :elb-logs key-list rows {:entities clojure-keyword->postgres-entity}))))




