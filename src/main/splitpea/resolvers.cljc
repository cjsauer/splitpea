(ns splitpea.resolvers
  "Resolvers shared by both client and server"
  (:require [com.wsscode.pathom.connect :as pc]))

(pc/defresolver primary-email
  [_ {:user/keys [email]}]
  {::pc/input  #{:user/email}
   ::pc/output #{:user/primary-email}}
  {:user/primary-email (if (coll? email)
                         (first email)
                         email)})

(pc/defresolver greeting
  [_ {:user/keys [primary-email]}]
  {::pc/input  #{:user/primary-email}
   ::pc/output #{:user/greeting}}
  {:user/greeting (str "Hello, " primary-email)})

(def all [primary-email
          greeting])
