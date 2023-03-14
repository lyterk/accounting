(ns lyterk.accounting-test
  (:require [clojure.test :refer :all]
            [lyterk.accounting :refer :all]
            [clojurewerkz.money.format :refer [format]]))

(deftest a-test
  (testing "Basic test"
    (is (= 1 1))))

(deftest import-test
  (testing "Failing weird"
    (is (= "Hello, Kevin!" (greet {:name "Kevin"})))))

(deftest money-test
  (testing "should money"
    (is (= "$1.00" (format (one-dollar))))))
