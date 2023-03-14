(ns lyterk.importing
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [java-time.api :as jt]
   [clojurewerkz.money.amounts :as ma]
   [clojurewerkz.money.currencies :as mc]
   [clojure.data.csv :as csv]))

(defn debug [x]
  (println x)
  x)

(defn keywordize
  "Given a string, lowercase it, remove whitespace, make it a keyword"
  [s]
  (as-> s t ;; Alias s to t
    (str/trim t)
    (str/lower-case t)
    (str/replace t #"\s+" "-")
    (keyword t)))

(defn ingest-money [amount & {:keys [currency]
                              :or {currency "USD"}}]
  (let [amount (Double/parseDouble amount)]
    (ma/amount-of (mc/for-code currency) amount)))

(defn csv->maps
  "Turn our csv bytestream into maps"
  [data]
  (map zipmap
       (->> (first data)
            (map keywordize)
            repeat)
       (rest data)))

(defn hasher [row fields]
  (let [values (map #(get row %) fields)]
    (assoc row :hash (hash values))))

(defn clear-empties [m]
  (reduce-kv
   (fn [m k v]
     (if (and
          (= (type v) java.lang.String)
          (str/blank? v))
       m
       (assoc m k v)))
   {}
   m))

(defn update-keyed
  "Given a map, and a map with the same keys to lambdas"
  [original modifier]
  (reduce-kv (fn [m k _]
               (let [key-fn (get modifier k identity)]
                 (update m k key-fn)))
             original original))

(defn load-csv
  [file-name entry-time & {:keys [munge-map munge-fn hashing-fields]
                           :or {munge-map {}
                                munge-fn identity
                                hashing-fields []}}]
  (with-open [rdr (io/reader file-name)]
    (doall
     (->> (csv/read-csv rdr)
          csv->maps
          (map
           #(-> %
                (update-keyed munge-map)
                munge-fn
                clear-empties
                (hasher hashing-fields)
                (assoc :entry-time entry-time)))))))

(def chase-credit-munge
  {:munge-map {:transaction-date #(jt/local-date "MM/dd/yyyy" %)
               :post-date #(jt/local-date "MM/dd/yyyy" %)
               :amount ingest-money}
   :munge-fn identity
   :hashing-fields [:date :amount :description]})

(defn ally-checking-rowwise [row]
  (let [datetime (jt/local-date-time (:date row) (:time row))
        row (dissoc row :date :time)]
    (assoc row :datetime datetime)))

(def ally-checking-munge
  {:munge-map {:date #(jt/local-date "yyyy-MM-dd" %)
               :time #(jt/local-time "HH:mm:ss" %)
               :amount ingest-money}
   :munge-fn ally-checking-rowwise
   :hashing-fields [:datetime :description :amount :type]})
