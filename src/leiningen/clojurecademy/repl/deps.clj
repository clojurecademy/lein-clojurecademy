(ns leiningen.clojurecademy.repl.deps
  (:require [kezban.core :refer :all]
            [leiningen.clojurecademy.util :as util]))

(defn- get-deps-from-ns
  [ns-form]
  (filter #(and (list? %) (keyword? (first %))) ns-form))

(defn get-deps
  [ns-form]
  (util/println-debug "*Getting dependencies...*\n")
  (let [deps (get-deps-from-ns ns-form)]
    (reverse (reduce #(conj %1 (cons (->> (first %2)
                                          str
                                          drop-first
                                          (apply str)
                                          symbol) (map (fn [s]
                                                         (cons 'quote [s])) (rest %2))))
                     '(do) deps))))