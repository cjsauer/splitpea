(ns splitpea.server
  (:require [datomic.ion.lambda.api-gateway :as apigw]
            [splitpea.authn.server :as authn]
            [splitpea.model :as model]
            [splitpea.resolvers :as shared]
            [splitpea.server.db :as db]
            [tightrope.server.ions :as irope]))

(def config
  {:path           "/api"
   :remote         {:ws-uri          "https://7ps9rxk22d.execute-api.us-east-1.amazonaws.com/dev"
                    :request->lookup authn/request->lookup}
   :parser-opts    {:env       {}
                    :resolvers (concat shared/resolvers
                                       db/resolvers
                                       authn/resolvers)}
   :authz          model/rules
   :schemas        [model/datomic-schema]
   :db-name        "splitpea-dev-db"
   :datomic-config {:server-type   :ion
                    :region        "us-east-1"
                    :system        "splitpea-dev"
                    :creds-profile "sandbox"
                    :endpoint      "http://entry.splitpea-dev.us-east-1.datomic.net:8182/"
                    :proxy-port    8182}})

(defn handler
  [req]
  (let [rope-handler (-> (irope/ion-handler config)
                         (authn/middleware))]
    (rope-handler req)))

(def ionized-handler
  (apigw/ionize handler))

(defn on-connect
  [input]
  (irope/on-connect config input))

(defn on-disconnect
  [input]
  (irope/on-disconnect config input))

(defn on-message
  [input]
  (irope/on-message config input))

(def ws-on-connect (apigw/ionize on-connect))
(def ws-on-disconnect (apigw/ionize on-disconnect))
(def ws-on-message (apigw/ionize on-message))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; REPL testing

(comment

  (require '[clojure.java.io :as io]
           '[clojure.core.async :as a]
           '[datomic.client.api :as d]
           '[splitpea.server.db :as db])

  (-> (handler
       {:request-method :post
        :uri "/api"
        :headers {"accept" "application/edn"
                  "content-type" "application/edn"
                  "authorization" (str "Token " "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyL2VtYWlsIjoiY2FsdmluIn0.MbD4J-389aG1UuY2xEioX2ujx4uqIr41ai59OR4DSIA")}
        ;; :body (io/input-stream (.getBytes (str [{`(authn/login! {:login/email "calvin"}) [:login/token]}])))})
        :body (io/input-stream (.getBytes (str [{[:db/id 15256823347019860] [:user/email :user/greeting]}])))})
      :body
      slurp
      )

  (defn load-sample-data
    []
    (let [tx-data [{:db/ensure    :team/validate
                    :team/slug    "blue-team"
                    :team/members [{:user/email "another"}
                                   {:user/email "calvin"}
                                   {:user/email "brittany"}
                                   {:user/email "billy"}]}
                   {:db/ensure    :team/validate
                    :team/slug    "red-team"
                    :team/members [{:user/email "derek"}]
                    }]]
      (irope/xact! config tx-data)
      ))

  (load-sample-data)

  (d/delete-database (irope/get-client (:datomic-config config)) {:db-name "splitpea-dev-db"})


  (db/entity-by (irope/get-db config) :user/email "calvin" '[*])

  (db/entity-by (irope/get-db config) :team/slug "red-team" '[*])

  (db/entity-by (irope/get-db config) :team/slug "blue-team" '[* {:team/members [*]}])

  (irope/xact! config [[:db/retract [:team/slug "red-team"]
                        :team/members [:user/email "calvin"]]])

  (d/pull (irope/get-db config) '[*] 77)

  (irope/xact! config [{:team/slug "red-team"
                        :team/members [[:user/email "calvin"]]}])

  (db/collaborators (irope/get-db config) [:user/email "calvin"] [:user/email])



  (irope/eid->lookups (irope/get-db config) 39424088925536341)

  (irope/xact! config [{:user/email "jerry"}])

  (irope/all-conn-ids (irope/get-db config))

  (d/q '[:find (pull ?cid [*])
         :in $
         :where [_ :aws.apigw.ws.conn/id ?cid]]
       (irope/get-db config))

  )
