(ns lyterk.ingestion.utils
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [java-time.api :as jt]
   [clojurewerkz.money.amounts :as ma]
   [clojurewerkz.money.currencies :as mc]
   [clojure.data.csv :as csv]))

(defn keywordize
  "Given a string, lowercase it, remove whitespace, make it a keyword"
  [s]
  (as-> s t ;; Alias s to t
    (str/trim t)
    (str/lower-case t)
    (str/replace t #"\s+" "-")
    (str/replace t #"[^a-z\-]" "")
    (keyword t)))

(defn parse-money [amt-str & {:keys [currency]
                              :or {currency "USD"}}]
  (let [assumed-currency (mc/for-code currency)
        checked-amt-str (-> amt-str
                            ;; Get rid of spaces/whitespace/commas
                            (str/replace #"[\s,]" "")
                            ;; Strike currency symbol (use this to validate
                            ;; currency is as expected?)
                            (str/replace (.getSymbol assumed-currency) ""))
        amount (Double/parseDouble checked-amt-str)]
    (ma/amount-of (mc/for-code currency) amount)))

(defn merge-datetime [row]
  (let [datetime (jt/local-date-time (:date row) (:time row))
        row (dissoc row :date :time)]
    (assoc row :datetime datetime)))

(defn header-preprocessor
  [[headers first-row & _]]
  headers)

(defn csv->maps
  "Turn our csv bytestream into maps"
  [data & {:keys [header-fn]
           :or {header-fn header-preprocessor}}]
  (map zipmap
       (->> header-preprocessor
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

(defn transform-row
  [field-transformations
   rowwise-fn
   hashing-fields
   key]
  (let [entry-time (jt/local-time)]
    (fn [row]
      (println (str "transform-row called: " row))
      (-> row
          clear-empties
          (update-keyed field-transformations)
          rowwise-fn
          (hasher hashing-fields)
          (assoc :entry-time entry-time :key key)))))

(defn load-csv
  [file-name & {:keys [header-fn field-transformations rowwise-fn hashing-fields key]
                :or {header-fn identity
                     field-transformations {}
                     rowwise-fn identity
                     hashing-fields []}}]
  (let [transform (transform-row field-transformations rowwise-fn hashing-fields key)]
    (with-open [rdr (io/reader file-name)]
      (doall
       (->> (csv/read-csv rdr)
            csv->maps
            (map 'transform))))))
