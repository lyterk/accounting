(ns lyterk.accounting
  (:gen-class)
  (:require [clojurewerkz.money.amounts :as ma]
            [clojurewerkz.money.currencies :as mc]
            [taoensso.timbre :refer [warn]]))

(defn one-dollar []
  (ma/amount-of mc/USD 1.0))

(defn greet
  "Callable entry point to the application."
  [data]
  (warn "hello there")
  (str "Hello, " (or (:name data) "World") "!"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))
