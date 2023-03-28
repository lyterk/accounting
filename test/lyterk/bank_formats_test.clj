(ns lyterk.ingestion.bank-formats-test
  (:require [clojure.test :as t]
            [lyterk.ingestion.bank-formats :refer :all]
            [lyterk.ingestion.utils :as lu]
            [java-time.api :as jt]
            [clojurewerkz.money.amounts :as ma]
            [clojurewerkz.money.currencies :as mc]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(t/deftest chase-preprocess-fn-test
  (t/testing "Header preprocessor should work on my dummy file"
    (with-open [rdr (io/reader "resources/tests/chase_invest_transactions.csv")]
      (let [csv-file (csv/read-csv rdr)
            expected
            ["Trade Date" "Post Date" "Settlement Date" "Account Name" "Account Number"
             "Account Type" "Type" "Description" "Cusip" "Ticker" "Security Type"
             "Local Currency" "Price Local Currency" "Price Local" "Quantity"
             "Cost Local Currency" "Cost Local" "G/L Short Local Currency"
             "G/L Short Local" "G/L Long Local Currencys" "G/L Long Local"
             "Amount Local Currency" "Amount Local" "Income Local Currency"
             "Income Local" "Balance" "Commissions Local Currency" "Commissions Local"
             "Tran Code" "Tran Code Description" "Broker" "Check Number" "Tax Withheld"]
            actual (chase-preprocess-fn csv-file)]
        (t/is (= expected actual))))))

(t/deftest chase-csv-test
  (t/testing "All together now"
    (let [now (jt/local-time)
          zero-usd (ma/amount-of mc/USD 0)
          expected-amount-sum (ma/amount-of mc/USD -261.18)
          chase-transformations (get-in chase [:credit :csv])
          rows (li/load-csv
                "resources/tests/visa_test.csv"
                now
                chase-transformations)
          amounts (map #(get % :amount zero-usd) rows)
          memos (filter #(not (nil? %)) (map :memo rows))]
      (t/is (= expected-amount-sum
               (reduce
                (fn [acc amt] (ma/plus acc amt))
                zero-usd
                amounts)))
      (t/is (= [] memos))
      ;; (t/is (= [] rows))
      (t/is (= 485841902 (reduce + 0 (map :hash rows)))))))

(t/deftest ally-csv-test
  (t/testing "Ally checking failures"
    (let [now (jt/local-time)
          zero-usd (ma/amount-of mc/USD 0)
          ally-transformations (get-in ally [:checking :csv])
          rows (li/load-csv
                "resources/tests/ally_test.csv"
                now
                ally-transformations)
          expected-amount (li/parse-money "43.13")
          amounts (map #(get % :amount zero-usd) rows)]
      (def amounts amounts)
      (t/is (= expected-amount (reduce (fn [acc amt] (ma/plus acc amt)) amounts))))))

(t/deftest venmo-csv-test
  (t/testing "Venmo checking stuff"
    (let [now (jt/local-time)
          venmo-transformations (get-in venmo [:csv])
          zero-usd (li/parse-money "0")
          rows (li/load-csv
                "resources/tests/venmo_test.csv"
                now
                venmo-transformations)
          amounts (map :amount-total rows)
          expected-amount (li/parse-money "-40227.00")]
      (t/is (= expected-amount (reduce
                                (fn [acc amt]
                                  (ma/plus acc amt)) amounts))))))

(t/deftest row-test
  (t/testing "Row should be munged"
    (let [row-positive {:transaction-date "02/16/2023"
                        :post-date "02/17/2023"
                        :description "Left alone"
                        :amount "123.45"}
          positive-transaction (li/update-keyed row-positive (get-in chase [:credit :csv :field-transformations]))]
      (t/is (= "Left alone" (get positive-transaction :description)))
      (t/is (= (ma/amount-of mc/USD 123.45) (get positive-transaction :amount)))
      (t/is (= (jt/local-date "MM/dd/yyyy" "02/16/2023") (get positive-transaction :transaction-date))))
    (let [row-negative {:transaction-date "02/16/2023"
                        :post-date "02/17/2023"
                        :description "Left alone"
                        :amount "-123.45"}
          negative-transaction (li/update-keyed row-negative (get-in chase [:credit :csv :field-transformations]))]
      (t/is (= "Left alone" (get negative-transaction :description)))
      (t/is (= (ma/amount-of mc/USD -123.45) (get negative-transaction :amount)))
      (t/is (= (jt/local-date "MM/dd/yyyy" "02/16/2023") (get negative-transaction :transaction-date))))))
