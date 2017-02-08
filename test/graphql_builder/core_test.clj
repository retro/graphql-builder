(ns graphql-builder.core-test
  (:require [clojure.test :refer :all]
            [graphql-builder.core :refer :all]
            [clojure.edn :as edn]
            [graphql-builder.core :as core]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def test-statements (map str/trim (edn/read-string (slurp "test/graphql_builder/resources/statements.edn"))))
(def parsed-statements (map core/parse test-statements))

#_ (defn render-parsed! [to-filename parsed-data]
     (pp/pprint parsed-data
                (clojure.java.io/writer (str "test/graphql_builder/resources/parsed/" to-filename))))
;; (render-parsed! "statements.edn" parsed-statements)

(deftest generate-test
  (is (= test-statements
         (map core/generate parsed-statements))))
