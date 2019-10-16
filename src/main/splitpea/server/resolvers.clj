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
  [{:keys [request]} _]
  {::pc/output #{:user/me}}
  (when-let [token (-> request :headers (get "authorization"))]
    (when-let [user (db/entity-by db :user/email token)]
      {:user/me user})))

(pc/defmutation login!
  [_ {:login/keys [email]}]
  {::pc/input  #{:login/handle}
   ::pc/output #{:login/token}}
  ;; TODO: email login link, token gathered from there
  ;; For now just pass it back directly
  {:login/token email})

(def all [db
          me
          login!])
