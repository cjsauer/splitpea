(ns splitpea.server
  (:require [tightrope.server :as rope]
            [splitpea.resolvers :as shared-resolvers]
            [splitpea.server.resolvers :as server-resolvers]
            [com.wsscode.pathom.connect :as pc]
            [clojure.core.async :as a]))

(def all-resolvers
  (concat shared-resolvers/all
          server-resolvers/all))

(def handler
  (rope/tightrope-handler
   {:parser-opts {:resolvers all-resolvers}
    :path "/api"
    }))


(comment

  (require '[clojure.java.io :as io])

  (let [parser (#'tightrope.server/default-parser {:resolvers all-resolvers})]
    (a/<!! (parser {} [{[:org/slug "carrot"] [{:org/sections [:section/name]}]}])))

  (handler
   {:request-method :post
    :uri "/api"
    :body (io/input-stream (.getBytes (str [{[:org/slug "carrot"] [:link/add]}])))})

  )
