(ns splitpea.server
  (:require [tightrope.server :as rope]
            [splitpea.resolvers :as shared-resolvers]
            [splitpea.server.resolvers :as server-resolvers]
            [splitpea.server.db :as db]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.datomic :as pcd]
            [com.wsscode.pathom.connect.datomic.client :refer [client-config]]
            ))

(defn custom-parser
  [resolvers env]
  (p/parser
   {::p/env     (merge
                 {::p/reader               [p/map-reader
                                            pc/reader2
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
                 env)
    ::p/mutate  pc/mutate
    ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                 (pcd/datomic-connect-plugin (assoc client-config ::pcd/conn (:conn env)))
                 p/error-handler-plugin
                 p/trace-plugin]}))

(defn handler
  [req]
  (let [all-resolvers (concat shared-resolvers/all
                              server-resolvers/all)
        rope-config   {:path "/api"
                       :parser (custom-parser all-resolvers {:conn (db/get-conn)})}
        rope-handler  (rope/tightrope-handler rope-config)]
    (rope-handler req)))


(comment

  (require '[clojure.java.io :as io]
           '[clojure.core.async :as a])

  (let [parser (custom-parser (concat shared-resolvers/all server-resolvers/all)
                              {:conn (db/get-conn)})]
    (parser {} [{[:team/slug "red-team"] [{:team/members [:user/email]}]}]))

  (handler
   {:request-method :post
    :uri "/api"
    :body (io/input-stream (.getBytes (str [{[:org/slug "carrot"] [:link/add]}])))})

  )
