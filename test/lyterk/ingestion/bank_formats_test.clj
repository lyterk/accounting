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
    (let [chase-transformations (get-in chase [:credit :csv])
          transform (lu/transform-row chase-transformations)
          input {:post-date "02/16/2023"
                 :transaction-date "02/17/2023"
                 :description "UPTOWN ESPRESSO BAR LTD"
                 :category "Food & Drink"
                 :type "Sale"
                 :amount "-12.91"}
          actual (transform input)]
      (t/is (=
             (lu/parse-money "-12.91" :currency "USD")
             (:amount actual))))))


(t/deftest ally-csv-test
  (t/testing "Ally checking failures"
    (let [ally-transformations (get-in ally [:checking :csv])
          transform (lu/transform-row ally-transformations)
          input {:date "2023-02-15"
                 :time "23:40:57"
                 :amount "1.06"
                 :type "Deposit"
                 :description "Interest Paid"}
          actual (transform input)]
      (t/is (=
             (lu/parse-money "1.06" :currency "USD")
             (:amount actual)))
      (t/is (=
             (jt/local-date-time iso-local-date-time "2023-02-15T23:40:57")
             (:datetime actual)))
      (t/is (=
             1327965429
             (:hash actual)))
      (t/is (=
             :ally-checking-account
             (:key actual))))))

(t/deftest venmo-csv-test
  (t/testing "Venmo checking stuff"
    (let [venmo-transformations (get-in venmo [:csv])
          transform (lu/transform-row venmo-transformations)
          input {:id "1111111111111111113"
                 :datetime "2023-03-21T07:19:00"
                 :type "Charge"
                 :status "Complete"
                 :note "Shwarma"
                 :source-person "Rick Santorum"
                 :dest-person "Chuck Schumer"
                 :amount-total "+ $16.50"
                 :amount-tip ""
                 :amount-tax "0"
                 :amount-fee ""
                 :tax-rate "0"
                 :tax-exempt ""
                 :funding-source ""
                 :destination "Venmo balance"
                 :beginning-balance ""
                 :ending-balance ""
                 :statement-period-venmo-fees ""
                 :terminal-location "Venmo"
                 :year-to-date-venmo-fees ""
                 :disclaimer ""}
          actual (transform input)]
      (t/is (=
             (lu/parse-money "16.50" :currency "USD")
             (:amount-total actual)))
      (t/is (=
             (jt/local-date-time iso-local-date-time "2023-03-21T07:19:00")
             (:datetime actual))))))
