(ns splitpea.server.ion
  (:require [tightrope.server :as rope]
            [datomic.ion.lambda.api-gateway :as apigw]
            [splitpea.server.db :as db]
            [splitpea.resolvers :as shared-resolvers]
            [splitpea.server.resolvers :as server-resolvers]))

(defn handler
  [req]
  (let [all-resolvers (concat shared-resolvers/all
                              server-resolvers/all)
        rope-config   {:path "/api"
                       :parser-opts {:env {:conn (db/get-conn)}
                                     :resolvers all-resolvers}}
        rope-handler  (rope/tightrope-handler rope-config)]
    (rope-handler req)))

(def ionized-handler
  (apigw/ionize handler))
