(ns leiningen.clojurecademy.repl.rule
  (:require [kezban.core :refer :all]
            [leiningen.clojurecademy.util :as util]
            [clojure.walk :as walk]
            [clojure.set :as s]))

(defn- check-one-fn
  [rule body]
  (when (and (:only-use-one-fn? rule) (or (not= (count body) 1) ((complement list?) (first body))))
    (util/runtime-ex "Rule `only use one fn` is enabled.You should define a function and you can't define more than 1 function.")))

(defn- namespace-sym->sym
  [coll]
  (map (comp symbol name) coll))

(defn- find-used-fns-in-required-fns!
  [required-fns used-fns body]
  (walk/postwalk (fn [s]
                   (when-let* [_ (symbol? s)
                               named-sym ((comp symbol name) s)
                               _ (util/in? named-sym required-fns)]
                              (swap! used-fns (fn [v]
                                                (conj v named-sym))))) body))

(defn- check-required-fn
  [rule body]
  (when (:required-fns rule)
    (check-one-fn rule body)
    (let [required-fns (set (namespace-sym->sym (:required-fns rule)))
          used-fns (atom #{})]
      (find-used-fns-in-required-fns! required-fns used-fns body)
      (when-not (= required-fns @used-fns)
        (util/runtime-ex (str "The function does not contain required fns. -> " (vec (s/difference required-fns @used-fns))))))))

(defn check-rules
  [route body]
  (util/println-debug "*Checking rules...*\n")
  (let [rule (-> route :subject :instruction :rule)]
    (check-one-fn rule body)
    (check-required-fn rule body)))

(defn get-restricted-fns
  [route]
  (let [rule (-> route :subject :instruction :rule)]
    (if (:restricted-fns rule)
      (namespace-sym->sym (:restricted-fns rule))
      [])))