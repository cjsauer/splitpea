(ns splitpea.server
  (:require [tightrope.server.ions :as irope]
            [splitpea.model :as model]
            [splitpea.resolvers :as shared]
            [splitpea.server.db :as db]
            [splitpea.authn.server :as authn]
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
           '[splitpea.server.db :as db]
           '[tightrope.server.ions.remote :as iremote])

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
      (d/transact (irope/get-conn config) {:tx-data tx-data})
      ))

  (load-sample-data)

  (db/entity-by (irope/get-db config) :user/email "calvin")

  (db/entity-by (irope/get-db config) :team/slug "red-team")

  (db/entity-by (irope/get-db config) :team/slug "blue-team" '[* {:team/members [*]}])

  (db/xact (irope/get-conn config) [[:db/retract [:team/slug "red-team"]
                                     :team/members [:user/email "calvin"]]])

  (db/collaborators (irope/get-db config) [:user/email "calvin"] [:user/email])

  (irope/subscribe! (irope/get-conn config) "c1" [:user/email "calvin"])

  (d/delete-database (irope/get-client (:datomic-config config)) {:db-name "splitpea-dev-db"})

  (iremote/unsubscribe-tx (irope/get-db config) "c1" [[:user/email "calvin"]])

  (d/transact (irope/get-conn config) {:tx-data [{:db/ident :user/age
                                                  :db/valueType :db.type/long
                                                  :db/cardinality :db.cardinality/one}]})

  (iremote/subscribe! (irope/get-conn config) "c1" [[:user/email "calvin"] [:user/email "brittany"]])

  (iremote/unsubscribe! (irope/get-conn config) "c1" [[:user/email "calvin"] [:user/email "brittany"]])

  (iremote/multiplex-datoms (irope/get-db config)
                            [[13194139533332 50 #inst "2019-10-23T02:23:04.997-00:00" 13194139533332 true]
                             [15256823347019860 85 28 13194139533332 true]
                             [39424088925536341 85 26 13194139533332 true]])

  (iremote/eid->lookups (irope/get-db config) 39424088925536341)

  (iremote/broadcast-datoms! (irope/get-db config)
                             [[13194139533332 50 #inst "2019-10-23T02:23:04.997-00:00" 13194139533332 true]
                              [15256823347019860 85 28 13194139533332 true]
                              [39424088925536341 85 26 13194139533332 true]])

  (let [datoms [[13194139533332 50 #inst "2019-10-23T02:23:04.997-00:00" 13194139533332 true]
                [15256823347019860 85 28 13194139533332 true]
                [39424088925536341 85 26 13194139533332 true]]]
    (iremote/make-lookup-table (irope/get-db config) datoms))


  (iremote/subscribe! (irope/get-conn config) "CFoeleW3oAMCFJQ=" [[:user/email "calvin"]])

  )
