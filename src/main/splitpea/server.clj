(ns splitpea.server
  (:require [tightrope.server.ions :as irope]
            [splitpea.model :as model]
            [splitpea.resolvers :as shared]
            [splitpea.server.db :as db]
            [splitpea.server.authn :as authn]
            [datomic.ion.lambda.api-gateway :as apigw]
            ))

(def config
  {:path           "/api"
   :parser-opts    {:env       {}
                    :resolvers (concat shared/resolvers
                                       db/resolvers
                                       authn/resolvers)}
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
        :body (io/input-stream (.getBytes (str [:user/me])))})
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
      (d/transact (irope/get-conn config) {:tx-data tx-data})
      ))

  (load-sample-data)

  (db/entity-by (irope/get-db config) :user/email "calvin")

  (db/entity-by (irope/get-db config) :team/slug "red-team")

  (db/entity-by (irope/get-db config) :team/slug "blue-team" '[* {:team/members [*]}])

  (db/xact (irope/get-conn config) [[:db/retract [:team/slug "red-team"]
                                     :team/members [:user/email "calvin"]]])

  (db/collaborators (irope/get-db config) [:user/email "calvin"] [:user/email])



  (d/delete-database (irope/get-client (:datomic-config config)) {:db-name "splitpea-dev-db"})

  )
