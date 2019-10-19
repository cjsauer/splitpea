(ns user
  (:require [datomic.ion.dev :as ion-dev]
            [clj-http.client :as http]
            [tightrope.dev :as rope-dev]))

(defn deploy
  [& [opts]]
  (rope-dev/ion-release
   (merge
    opts
    {:region        "us-east-1"
     :creds-profile "sandbox"
     :group         "splitpea-dev-compute"})))

(defn apigw-request
  [payload]
  (-> (http/post "https://6bp2dwy85m.execute-api.us-east-1.amazonaws.com/dev/api"
                 {:content-type "application/edn"
                  :accept       "application/edn"
                  :body         (str payload)})
      :body
      read-string))

(comment
  (apigw-request [{[:team/slug "red-team"] [:team/members]}])

  )
