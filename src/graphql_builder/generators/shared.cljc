(ns graphql-builder.generators.shared
  (:require [clojure.string :as str]
            [graphql-builder.util :as util]))

(defn quote-arg [v]
  (if (string? v)
    (if (= (str/upper-case v) v) v (str "\"" v "\""))
    v))

(defn object-default-value [value]
  (str "{ "
       (str/join ", "(map (fn [v] (str (:name v) ": " (quote-arg (:value v)))) value))
       " }"))

(defn argument-value-value [value]
  (cond
    (:values value) (str "[" (str/join ", " (map quote-arg (:values value))) "]")
    (and (vector? value) (= :object-value (first value))) (object-default-value (last value))
    :else(quote-arg value)))

(defn add-var-prefix [prefix name]
  (if prefix
    (str prefix "__" name)
    name))

(defn argument-value [argument config]
  (let [value (:value argument)
        variable-name (:variable-name argument)]
    (cond
      (boolean value) (argument-value-value value)
      (boolean variable-name) (str "$" (add-var-prefix (:prefix config) variable-name)))))

(defn argument-name [argument config]
  (let [prefix (:prefix config)
        name (:argument-name argument)]
    (add-var-prefix prefix name)))

(defn node-arguments [node config]
  (when-let [arguments (:arguments node)]
    (str "("
         (str/join ", " (map #(str (:argument-name %) ": " (argument-value % config)) arguments))
         ")")))

(defn directive [d config]
  (str "@" (:name d) (node-arguments d config)))

(defn directives [node config]
  (when-let [ds (:directives node)]
    (str " " (str/join " " (map (fn [d] (directive d config)) ds)))))

(defn fragment-type-name [node]
  (when-let [name (get-in node [:type-condition :type-name])]
    (str " on " name)))

(defn has-children? [node]
  (boolean (seq (:selection-set node))))

(defn open-block [node]
  (when (has-children? node) " {"))

(defn close-block [node indent-level]
  (when (has-children? node) (util/indent indent-level "}")))
