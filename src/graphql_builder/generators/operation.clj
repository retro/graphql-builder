(ns graphql-builder.generators.operation
  (:require [graphql-builder.util :as util]
            [clojure.string :as str]
            [graphql-builder.generators.shared :refer [quote-arg]]))

(defn object-default-value [value]
  (str " = { "
       (str/join ", "(map (fn [v] (str (:name v) ": " (quote-arg (:value v)))) value))
       " }"))

(defn default-value [variable]
  (when-let [d (:default-value variable)]
    (let [val @d]
      (if (and (vector? val) (= :object-value (first val)))
        (object-default-value (last val))
        (str " = " (quote-arg val))))))

(defn variable-value [variable]
  (let [type-name @(:type-name variable)
        required? (:required variable)]
    (str type-name (when required? "!") (default-value variable))))

(defn node-variables [node]
  (when-let [variables (:variable-definitions node)]
    (str "("
         (str/join ", " (map #(str "$" @(:variable-name %) ": " (variable-value %)) variables))
         ")")))

(defn operation-name [operation]
  (let [{:keys [type name]} (:operation-type operation)]
    (if name
      (str type " " name)
      type)))

(defn generator [visitor config indent-level node]
  (let [selection-set (:selection-set node)]
    [(util/indent indent-level (str (operation-name node) (node-variables node) " {"))
     (visitor config (inc indent-level) selection-set)
     (util/indent indent-level "}")]))
