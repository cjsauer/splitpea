(ns splitpea.server.ion
  (:require [splitpea.server :as server]))

(def ionized-handler
  (apigw/ionize server/handler))
