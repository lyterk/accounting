(ns lyterk.importing-test
  (:require [clojure.test :refer :all]
            [lyterk.importing :refer :all]
            [clojurewerkz.money.amounts :as ma]
            [clojurewerkz.money.currencies :as mc]
            [java-time.api :as jt]))

(deftest keywordize-test
  (testing "Keywordize not working as expected"
    (is (= :hello-there-kevin (keywordize "Hello There KevIN")))))

(deftest csv->maps-test
  (testing "Should turn the first row into the keys of the subsequent map"
    (let [expected [{:a 1 :b 2} {:a 3 :b 4}]
          actual (csv->maps [["a" "b"] [1 2] [3 4]])]
      (is (= expected actual)))))

(deftest clear-empties-test
  (testing "Should strike blank String values from map"
    (let [first {:a 1 :b "two" :c ""}
          first-updated {:a 1 :b "two"}
          second {:a 1 :b "two" :c []}
          third [first second]]
      (is (= {:a 1 :b "two"} (clear-empties first)))
      (is (= second (clear-empties second)))
      (is (= [first-updated second] (map clear-empties third))))))

(deftest ingest-money-test
  (testing "Should just take in the money amounts"
    (is (= 123 (ma/major-of (ingest-money "123.45"))))
    (is (= -123 (ma/major-of (ingest-money "-123.45"))))
    (is (= mc/USD (ma/currency-of (ingest-money "123.45"))))
    (is (= 12345 (ma/minor-of (ingest-money "123.45"))))))

(deftest row-test
  (testing "Row should be munged"
    (let [row-positive {:transaction-date "02/16/2023"
                        :post-date "02/17/2023"
                        :description "Left alone"
                        :amount "123.45"}
          positive-transaction (update-keyed row-positive (:munge-map chase-credit-munge))]
      (is (= "Left alone" (get positive-transaction :description)))
      (is (= (ma/amount-of mc/USD 123.45) (get positive-transaction :amount)))
      (is (= (jt/local-date "MM/dd/yyyy" "02/16/2023") (get positive-transaction :transaction-date))))
    (let [row-negative {:transaction-date "02/16/2023"
                        :post-date "02/17/2023"
                        :description "Left alone"
                        :amount "-123.45"}
          negative-transaction (update-keyed row-negative (:munge-map chase-credit-munge))]
      (is (= "Left alone" (get negative-transaction :description)))
      (is (= (ma/amount-of mc/USD -123.45) (get negative-transaction :amount)))
      (is (= (jt/local-date "MM/dd/yyyy" "02/16/2023") (get negative-transaction :transaction-date))))))

(deftest chase-csv-test
  (testing "All together now"
    (let [now (jt/local-time)
          zero-usd (ma/amount-of mc/USD 0)
          expected-amount-sum (ma/amount-of mc/USD -261.18)
          rows (load-csv
                "resources/tests/visa_test.csv"
                now
                chase-credit-munge)
          amounts (map #(get % :amount zero-usd) rows)
          memos (filter #(not (nil? %)) (map :memo rows))]
      (is (= expected-amount-sum
             (reduce
              (fn [acc amt] (ma/plus acc amt))
              zero-usd
              amounts)))
      (is (= [] memos))
      (is (= 485841902 (reduce + 0 (map :hash rows)))))))

(deftest ally-csv-test
  (testing "Ally checking failures"
    (let [now (jt/local-time)
          zero-usd (ma/amount-of mc/USD 0)
          expected-amount-sum (ma/amount-of mc/USD 0)
          rows (load-csv
                "resources/tests/ally_test.csv"
                now
                ally-checking-munge)
          expected-amount (ingest-money "12693.13")
          amounts (map #(get % :amount zero-usd) rows)]
      (is (= expected-amount (reduce (fn [acc amt] (ma/plus acc amt)) amounts))))))
