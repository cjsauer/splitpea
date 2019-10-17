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
  (when-let [token (-> request :headers (get "authorization"))]
    (when-let [user (db/entity-by db :user/email token)]
      {:user/me user})))

(pc/defmutation login!
  [_ {:login/keys [email]}]
  {::pc/input  #{:login/email}
   ::pc/output #{:user/me}}
  ;; TODO: email login link, token gathered from there
  ;; For now just pass it back directly
  {:user/me {:user/email email}})

(def all [db
          me
          login!])
