(ns leiningen.clojurecademy.repl.initial-code
  (:require [leiningen.clojurecademy.util :as util]))

(defn get-initial-code
  [route]
  (util/println-debug "*Getting initial code...*\n")
  (let [initial-code (-> route :subject :instruction :initial-code)
        forms (some-> initial-code :form util/wrap-code read-string)]
    (if (= 'ns (ffirst forms))
      (cons 'do (rest forms))
      (cons 'do forms))))
