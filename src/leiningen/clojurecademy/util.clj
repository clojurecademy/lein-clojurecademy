(ns leiningen.clojurecademy.util
  (:require [clojurecademy.dsl.validator :as validator]
            [clojure.tools.namespace.find :as f]
            [colorize.core :as cc]
            [leiningen.clojurecademy.conf :as conf]
            [clojure.pprint :as pp])
  (:import (java.io File ByteArrayOutputStream OutputStreamWriter)
           (java.nio.file FileSystem)))

(defn println-error
  [& args]
  (println (cc/color :red (apply str args))))

(defn println-warn
  [& args]
  (println (cc/color :magenta (apply str args))))

(defn println-info
  [& args]
  (println (cc/color :blue (apply str args))))

(defn println-success
  [& args]
  (println (cc/color :green (apply str args))))

(defn ex-msg
  [e]
  (.getMessage e))

(defn arg-ex
  [^String message]
  (throw (IllegalArgumentException. message)))

(defn runtime-ex
  [^String message]
  (throw (RuntimeException. message)))

(defn valid-course-map?
  [course-map]
  (try
    (validator/validate course-map)
    (println-success "Map is valid.")
    true
    (catch Exception e
      (println-error "Map is not valid. " (ex-msg e))
      false)))

(defn get-nsses-under-src
  [project]
  (f/find-namespaces-in-dir (File. ^String (str (:root project) (File/separator) "src"))))

(defn lisp?
  [form]
  (if (string? form)
    (try
      (read-string (str "(\n" form "\n)"))
      true
      (catch Exception _
        false))
    false))

(defn in?
  [x coll]
  (some #(= x %) coll))

(defn println-debug
  [& args]
  (when conf/debug-mode
    (println-info (apply str args))))

(defn pprint-debug
  [object]
  (when conf/debug-mode
    (pp/pprint object)))

(defn list-gen
  [& args]
  (reverse (reduce #(cons %2 %1) (reverse (first args)) (rest args))))

(def stdout (atom ""))

(defmacro console->sdtout
  [form]
  `(let [boutput# (ByteArrayOutputStream.)]
     (binding [*out* (OutputStreamWriter. boutput#)]
       ~form)
     (swap! stdout (constantly (str boutput#)))))


(defn form-eval
  [f coll]
  (let [sym f
        f   (resolve f)]
    {:form   (str (cons sym coll))
     :result (apply f coll)}))

(defn macro?
  [f]
  (-> f resolve meta :macro boolean))

(defn find-index-route
  [x coll]
  (letfn [(path-in [y]
            (cond
              (= y x) '()
              (coll? y) (let [[failures [success & _]]
                              (->> y
                                   (map path-in)
                                   (split-with not))]
                          (when success (cons (count failures) success)))))]
    (path-in coll)))

(defn nnth
  [coll index & indices]
  (reduce #(nth %1 %2) coll (cons index indices)))

(defn print-console
  [msg coll]
  (let [dashes (apply str (repeat 50 "="))]
    (println)
    (println dashes)
    (println msg)
    (println dashes)
    (doseq [x coll]
      (println x))
    (println dashes "\n")))

(defn wrap-code
  [code]
  (str "(\n" code "\n)"))