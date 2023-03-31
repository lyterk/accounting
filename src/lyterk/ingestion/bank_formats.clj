(ns lyterk.ingestion.bank-formats
  (:require [java-time.api :as jt]
            [java-time.format :as jtf]
            [lyterk.ingestion.utils :refer [merge-datetime parse-money]]
            [clojure.string :as str]))

(defonce iso-local-date-time (get jtf/predefined-formatters "iso-local-date-time"))

(defn ingest-common-date [s]
  (jt/local-date "MM/dd/yyyy" s))

(defn ingest-common-datetime [s]
  (jt/local-date-time iso-local-date-time s))

(defn chase-preprocess-fn [[headers first-row & _]]
  (let [zm (zipmap headers first-row)
        local-currency (get zm "Local Currency")]
    (into
     []
     (map #(str/replace % local-currency "Local Currency") headers))))

(defonce chase
  {:credit
   {:csv
    {:field-transformations {:transaction-date ingest-common-date
                             :post-date ingest-common-date
                             :amount parse-money}
     :key :chase-credit-card
     :hashing-fields [:date :amount :description]}}
   :investment
   {:csv
    {:field-transformations {:trade-date ingest-common-date
                             :post-date ingest-common-date
                             :settlement-date ingest-common-date}}}})

(defonce ally
  {:checking
   {:csv
    {:field-transformations {:date #(jt/local-date "yyyy-MM-dd" %)
                             :time #(jt/local-time "HH:mm:ss" %)
                             :amount parse-money}
     :key :ally-checking-account
     :rowwise-fn merge-datetime
     :hashing-fields [:datetime :description :amount :type]}}})

(defonce venmo
  {:csv
   {:field-transformations {:datetime ingest-common-datetime
                            :amount-total parse-money
                            :amount-tax parse-money
                            :amount-tip parse-money
                            :amount-fee parse-money
                            :beginning-balance parse-money
                            :statement-period-venmo-fees parse-money
                            :year-to-date-venmo-fees parse-money}
    :key :venmo-account
    :rename [[:from :source-person]
             [:to :dest-person]]
    :hashing-fields [:id]}})
