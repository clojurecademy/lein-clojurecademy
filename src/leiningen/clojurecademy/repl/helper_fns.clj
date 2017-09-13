(ns leiningen.clojurecademy.repl.helper-fns
  (:require [clojure.repl :as r]
            [leiningen.clojurecademy.util :as util]
            [leiningen.clojurecademy.repl.deps :as deps]))

(defn get-helper-fns-source
  [helper-fns]
  (util/println-debug "*Getting helper fns source...*\n")
  (when helper-fns
    (let [helper-fn-data (read-string (str "(\n" helper-fns "\n)"))
          ns-form        (first helper-fn-data)]
      (util/list-gen (deps/get-deps ns-form) (cons 'do (rest helper-fn-data))))))