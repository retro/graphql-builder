(ns graphql-builder.generators.operation
  (:require [graphql-builder.util :as util :refer [combine-children]]
            [clojure.string :as str]
            [graphql-builder.generators.shared :refer [quote-arg add-var-prefix object-default-value]]))

(defn default-value [variable]
  (when-let [val (:default-value variable)]
    (if (and (vector? val) (= :object-value (first val)))
      (str " = " (object-default-value (last val)))
      (str " = " (quote-arg val)))))

(defn variable-value [variable]
  (let [type-name (if (= :list (:node-type variable))
                    (str "[" (get-in variable [:inner-type :type-name]) "]")
                    (:type-name variable))
        required? (:required variable)]
    (str type-name (when required? "!") (default-value variable))))

(defn variable-name [variable config]
  (let [prefix (:prefix config)
        name (:variable-name variable)]
    (add-var-prefix prefix name)))

(defn node-variables-body [node config]
  (when-let [variables (:variable-definitions node)]
    (str/join ", " (map #(str "$" (variable-name % config) ": " (variable-value %)) variables))))

(defn node-variables [node config]
  (when (:variable-definitions node)
    (str "("
         (node-variables-body node config)
         ")")))

(defn operation-name [operation]
  (let [{:keys [type name]} (:operation-type operation)]
    (if name
      (str type " " name)
      type)))

(defn add-prefix-to-selection-node [prefix node]
  (let [name (or (:name node) (:field-name node))]
    (assoc node :name (add-var-prefix prefix name))))

(defn children [node config]
  (let [node-type (get-in node [:operation-type :type])
        children (:selection-set node)
        prefix (:prefix config)]
    (if (and (= "query" node-type) prefix)
      (map #(add-prefix-to-selection-node prefix %) children)
      children)))

(defn generate [visitor deps config indent-level node]
  [(util/indent indent-level
                (str (operation-name node) (node-variables node config) " {"))
   (visitor deps config (inc indent-level) (children node config))
   (util/indent indent-level "}")])

(defn generate-for-composition [visitor deps config indent-level node]
  {:variables (node-variables-body node config)
   :children (visitor deps config (inc indent-level) (children node config))})
