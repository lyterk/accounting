(ns lyterk.ingestion.accounting-test
  (:require [clojure.test :refer :all]
            [lyterk.ingestion.accounting :refer :all]))

(deftest a-test
  (testing "Basic test"
    (is (= 1 1))))

(deftest import-test
  (testing "Failing weird"
    (is (= "Hello, Kevin!" (greet {:name "Kevin"})))))
