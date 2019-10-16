(ns splitpea.resolvers
  "Resolvers shared by both client and server"
  (:require [com.wsscode.pathom.connect :as pc]))

(pc/defresolver greeting-resolver
  [_ {:user/keys [email]}]
  {::pc/input  #{:user/email}
   ::pc/output #{:user/greeting}}
  {:user/greeting (str "Hello, " (or (first email) email))})

(def all [greeting-resolver])
