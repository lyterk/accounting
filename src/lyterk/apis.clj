(ns lyterk.ingestion.apis
  (:require
   [clojure.edn :as edn]
   [cheshire.core :as json]
   [clj-http.client :as client]
   [oauth.client :as oauth]))

(defonce ^:private ^:const secrets (edn/read-string (slurp ".env.edn")))
(defonce ^:private ^:const ally-secrets (:ally secrets))
(defonce ^:private ally-consumer
  (oauth/make-consumer
   (:consumer-key ally-secrets)
   (:consumer-secret ally-secrets)
   "https://devapi.invest.ally.com/oauth/request_token"
   "https://devapi.invest.ally.com/oauth/access_token"
   nil
   :hmac-sha1))

(defn ally-get [endpoint & {:keys [params]
                            :or {params ""}}]
  (let  [base-url "https://devapi.invest.ally.com/v1/%s.json"
         full-url (format base-url endpoint)
         credentials (oauth/credentials
                      ally-consumer
                      (:oauth-token ally-secrets)
                      (:oauth-token-secret ally-secrets)
                      :get
                      full-url)
         auth (oauth/build-request credentials)
         response (client/get full-url auth :as :json :debug true)]
    (if (= 200 (:status response))
      (json/parse-string (:body response) true)
      response)))

;; (def accounts (ally-get "accounts"))
(def history (ally-get
              (format "accounts/%s/history" (:account-id ally-secrets))
              {:query-params
               {:range "all" :transactions "all"}}))
