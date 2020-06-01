(ns graphql-builder.util
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [camel-snake-kebab.core :refer [->camelCase]]))

(defn nl-join [coll]
  (when (seq coll)
    (str/join "" (vec (remove nil? coll)))))

(defn indent [level line]
  (str (str/join "" (repeat (* 2 level) " ")) line))

(defn combine-children [children]
  (reduce (fn [acc c]
            (let [children (or (:children acc) [])
                  deps (or (:deps acc) [])
                  c-deps (:deps c)]
              (assoc acc
                     :children (conj children (:children c))
                     :deps (if c-deps (into deps c-deps) deps))))
          {} children))

(defn transform-keys
  "Recursively transforms all map keys in coll with t."
  [t coll]
  (let [f (fn [[k v]] [(t k) v])]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) coll)))

(defn variables->graphql [vars]
  (transform-keys (comp ->camelCase name) vars))

(defn reverse-map
  "Reverse the keys/values of a map"
  [m]
  (into {} (map (fn [[k v]] [v k]) m)))
