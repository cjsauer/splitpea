(ns splitpea.server.resolvers
  "Server-only resolvers"
  (:require [com.wsscode.pathom.connect :as pc]
            [splitpea.server.db :as db]
            [datomic.client.api :as d]))

(pc/defresolver db
  [{:keys [conn]} _]
  {::pc/output #{::db}}
  {::db (d/db conn)})

(pc/defresolver me
  [{:keys [request]} {::keys [db]}]
  {::pc/input  #{::db}
   ::pc/output #{:user/me}}
  (when-let [authz (-> request :headers (get "authorization"))]
    (when-let [user (db/entity-by db :user/email authz)]
      {:user/me user})))

(pc/defmutation login!
  [_ {:login/keys [handle]}]
  {::pc/input #{:login/handle}
   ::pc/output #{:user/me}}
  (println "Checking handle: " handle)
  (when-let [user (get @users handle)]
    (println "Loggin in user: " handle)
    {:user/me user}))

(def all [db
          me
          login!])
