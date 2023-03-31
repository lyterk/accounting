(ns lyterk.ingestion.utils-test
  (:require [clojure.test :as t]
            [lyterk.ingestion.utils :refer :all]
            [clojurewerkz.money.amounts :as ma]
            [clojurewerkz.money.currencies :as mc]))

(t/deftest keywordize-test
  (t/testing "Keywordize not working as expected"
    (t/is (= :hello-there-kevin (keywordize "Hello There KevIN")))
    (t/is (= :amount-tax (keywordize "Amount (Tax)")))))

(t/deftest csv->maps-test
  (t/testing "Should turn the first row into the keys of the subsequent map"
    (let [expected [{:a 1 :b 2} {:a 3 :b 4}]
          actual (csv->maps [["a" "b"] [1 2] [3 4]])]
      (t/is (= expected actual)))))

(t/deftest clear-empties-test
  (t/testing "Should strike blank String values from map"
    (let [first {:a 1 :b "two" :c ""}
          first-updated {:a 1 :b "two"}
          second {:a 1 :b "two" :c []}
          third [first second]]
      (t/is (= {:a 1 :b "two"} (clear-empties first)))
      (t/is (= second (clear-empties second)))
      (t/is (= [first-updated second] (map clear-empties third))))))

(t/deftest money-test
  (t/testing "Should just take in the parse-money amounts"
    (t/is (= 123 (ma/major-of (parse-money "123.45"))))
    (t/is (= -123 (ma/major-of (parse-money "-123.45"))))
    (t/is (= mc/USD (ma/currency-of (parse-money "123.45"))))
    (t/is (= 12345 (ma/minor-of (parse-money "123.45"))))
    (t/is (= (bigint -123456) (ma/minor-of (parse-money "- $1,234.56"))))
    (t/is (= (bigint -4025000) (ma/minor-of (parse-money "- $40,250.00"))))
    (t/is (= (bigint 4025000) (ma/minor-of (parse-money "+ $40,250.00"))))
    (t/is (= 0 (ma/minor-of (parse-money "0"))))))
