(ns splitpea.server.ion
  (:require [tightrope.server :as rope]
            [datomic.ion.lambda.api-gateway :as apigw]))

(def handler
  (rope/tightrope-handler {:path "/api"
                           :parser-opts {:env {}
                                         :resolvers []}}))

(def ionized-handler
  (apigw/ionize handler))
