(ns splitpea.server
  (:require [tightrope.server :as rope]
            [splitpea.resolvers :as shared-resolvers]
            [splitpea.server.resolvers :as server-resolvers]
            [splitpea.server.db :as db]))

(defn handler
  [req]
  (let [all-resolvers (concat shared-resolvers/all
                              server-resolvers/all)
        rope-config   {:path "/api"
                       :parser-opts {:env {:conn (db/get-conn)}
                                     :resolvers all-resolvers}}
        rope-handler  (rope/tightrope-handler rope-config)]
    (rope-handler req)))


(comment

  (require '[clojure.java.io :as io])

  (let [parser (#'tightrope.server/default-parser {:resolvers all-resolvers})]
    (a/<!! (parser {} [{[:org/slug "carrot"] [{:org/sections [:section/name]}]}])))

  (handler
   {:request-method :post
    :uri "/api"
    :body (io/input-stream (.getBytes (str [{[:org/slug "carrot"] [:link/add]}])))})

  )
