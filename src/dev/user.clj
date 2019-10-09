(ns user
  (:require [datomic.ion.dev :as ion-dev]))

(def group "splitpea-dev-compute")

(defn release
  "Do push and deploy of app.  Supports stable and unstable releases.  Returns when deploy finishes running."
  [args]
  (try
    (let [push-data   (ion-dev/push args)
          deploy-args (merge (select-keys args [:creds-profile :region :uname])
                             (select-keys push-data [:rev])
                             {:group group})]
      (let [deploy-data        (ion-dev/deploy deploy-args)
            deploy-status-args (merge (select-keys args [:creds-profile :region])
                                      (select-keys deploy-data [:execution-arn]))]
        (loop []
          (let [status-data (ion-dev/deploy-status deploy-status-args)]
            (if (= "RUNNING" (:code-deploy-status status-data))
              (do (Thread/sleep 5000) (recur))
              status-data)))))
    (catch Exception e
      {:deploy-status "ERROR"
       :message       (.getMessage e)})))

(defn deploy
  []
  (release {:creds-profile "sandbox"
            :region "us-east-1"}))
