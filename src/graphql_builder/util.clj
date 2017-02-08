(ns graphql-builder.util
  (:require [clojure.string :as str]))

(defn nl-join [coll]
  (str/join "\n" (vec (remove nil? coll))))

(defn indent [level line]
  (str (str/join "" (repeat (* 2 level) " ")) line))
