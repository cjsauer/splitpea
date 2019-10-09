(ns splitpea.server.db
  (:require [datomic.client.api :as d]
            [splitpea.model :as model]))

(def cfg {:server-type :ion
          :region "us-east-1"
          :system "splitpea-dev"
          :creds-profile "sandbox"
          :endpoint "http://entry.splitpea-dev.us-east-1.datomic.net:8182/"
          :proxy-port 8182})

(def get-client
  "This function will return a local implementation of the client
  interface when run on a Datomic compute node. If you want to call
  locally, fill in the correct values in the map."
  (memoize #(d/client cfg)))

(defn- has-ident?
  [db ident]
  (contains? (d/pull db {:eid ident :selector [:db/ident]})
             :db/ident))

(defn- data-loaded?
  [db]
  (has-ident? db (-> model/datomic-schema first :db/ident)))

(defn- load-schema
  [conn]
  (let [db (d/db conn)]
    (if (data-loaded? db)
      :already-loaded
      (d/transact conn {:tx-data model/datomic-schema}))))

(defn ensure-dataset
  "Ensure that a database named db-name exists, running setup-fn
  against a connection. Returns connection"
  [db-name setup-sym]
  (require (symbol (namespace setup-sym)))
  (let [setup-var (resolve setup-sym)
        client (get-client)]
    (when-not setup-var
      (throw (ex-info (str "Could not resolve " setup-sym))))
    (d/create-database client {:db-name db-name})
    (let [conn (d/connect client {:db-name db-name})
          db (d/db conn)]
      (setup-var conn)
      conn)))

(defn get-conn
  []
  (ensure-dataset "splitpea-dev-db" `load-schema))

(comment

  (let [my-user {:db/ensure :user/validate
                 :user/email "calvin"
                 ;; :team/slug "test"
                 ;; :team/members [[:user/emails "calvin"] [:user/emails "brittany"]]
                 }]
    (d/transact (get-conn) {:tx-data [my-user]}))

  (d/pull (d/db (get-conn)) '[*] [:user/email "calvin"])

  (d/pull (d/db (get-conn)) '[{:team/members [:user/emails]}] [:team/slug "test"])

  (get-conn)

  (d/delete-database (get-client) {:db-name "splitpea-dev-db"})

  )
