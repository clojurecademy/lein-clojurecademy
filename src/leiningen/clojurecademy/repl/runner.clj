(ns leiningen.clojurecademy.repl.runner
  (:require [leiningen.clojurecademy.sandbox :as sandbox]
            [leiningen.clojurecademy.util :as util]
            [leiningen.clojurecademy.repl.deps :as deps]
            [leiningen.clojurecademy.repl.helper-fns :as helper-fns]
            [leiningen.clojurecademy.repl.initial-code :as initial-code]
            [leiningen.clojurecademy.repl.java-imports :as java-imports]
            [leiningen.clojurecademy.repl.rule :as rule]
            [leiningen.clojurecademy.repl.sub-instructions :as sub-instructions]
            [leiningen.clojurecademy.conf :as conf]
            [clojure.set :as set]
            [clojure.pprint :as pp])
  (:import (java.util.concurrent TimeoutException)))


(def sb-ns-prefix "sandbox-ns-")
(def sb-helper-ns-prefix "sandbox-helper-ns-")

(defn- wrap-ex
  [form err-msg]
  (util/list-gen '(try)
                 form
                 (util/list-gen '(catch Exception e)
                                (util/list-gen '(throw)
                                               (util/list-gen '(RuntimeException.)
                                                              (util/list-gen '(str)
                                                                             err-msg
                                                                             '(.getMessage e)))))))

(defn- get-sb-code-ds
  [test-var route ns-form body-without-ns]
  (let [complete-ds (util/list-gen '(do)
                                   (wrap-ex (deps/get-deps ns-form) "Something went wrong when installing dependencies.")
                                   (wrap-ex (initial-code/get-initial-code route) "Something went wrong when installing initial code.")
                                   (wrap-ex (java-imports/get-java-deps ns-form) "Something went wrong when checking Java dependencies.")
                                   (wrap-ex body-without-ns "Something went wrong when installing client code.")
                                   (sub-instructions/get-sub-ins-tests-execution-ds test-var route))]
    (when conf/debug-advanced
      (util/println-debug "Complete Data Structure:\n")
      (util/pprint-debug complete-ds)
      (println (apply str (repeat 50 "-")) "\n"))
    complete-ds))

(defn- get-body-form
  [route body]
  (util/println-debug "*Getting body form...*\n")
  (if (= 'ns (and (list? (first body)) (ffirst body)))
    body
    (let [ns* (some-> route :subject :instruction :initial-code :form util/wrap-code read-string)]
      (if (= 'ns (ffirst ns*))
        (cons ns* body)
        (cons (util/list-gen '(ns) (-> route :subject :ns)) body)))))

(defn- get-test-failed-msg
  [test-var route e]
  (format "\nTest failed: `%s` - route: %s\n`defcoursetest` file: %s - Line: %d\n\n  Error: %s\n\n"
          (-> test-var meta :name)
          (-> route :route-vec)
          (-> test-var meta :file)
          (-> test-var meta :line)
          (.getMessage e)))

(defn check-helper-fns
  [helper-fns-ds]
  (let [helper-checker-ns (gensym "helper-checker-ns-")]
    (try
      ((sandbox/make-sandbox-for-helpers helper-checker-ns) helper-fns-ds)
      (catch Throwable t
        (remove-ns helper-checker-ns)
        (throw (RuntimeException. ^String (str "Your helper fns are invalid : " (.getMessage t))))))))

(defn create-helper-fns-ns
  [sb-ns sb-helper-ns helper-fns-ds]
  (binding [*ns* (create-ns sb-helper-ns)]
    (clojure.core/refer-clojure)
    (eval helper-fns-ds)
    (binding [*ns* (the-ns sb-ns)]
      (refer sb-helper-ns))))

(defn eval-test
  [route test-var helper-fns]
  (if test-var
    (try
      (util/println-debug "\n============================================================\n")
      (util/println-debug (format "route: %s\ndefcoursetest: %s\nFile: %s\n" (:route-vec route)
                                  (-> test-var meta :name)
                                  (-> test-var meta :file)))
      (let [{:keys [name route-vec body]} @test-var
            body-form       (get-body-form route body)
            rest-of-body    (rest body-form)
            ns-form         (first body-form)
            body-without-ns (cons 'do rest-of-body)]
        (rule/check-rules route rest-of-body)
        (let [helper-fns-ds (or (helper-fns/get-helper-fns-source helper-fns) '(do))
              _             (check-helper-fns helper-fns-ds)
              sb-ns         (symbol sb-ns-prefix)
              sb-helper-ns  (symbol sb-helper-ns-prefix)
              sb            (sandbox/make-sandbox sb-ns body-form (rule/get-restricted-fns route))
              _             (create-helper-fns-ns sb-ns sb-helper-ns helper-fns-ds)
              before        (set (vals (ns-map (the-ns sb-ns))))
              all-ds        (get-sb-code-ds test-var route ns-form body-without-ns)
              result        (sb all-ds)
              after         (set (vals (ns-map (the-ns sb-ns))))]
          (remove-ns sb-ns)
          (remove-ns sb-helper-ns)
          (when-let [f (first (set/difference before after))]
            (throw (RuntimeException. ^String (str "You can not override built-in function: `" (-> f meta :name) "`"))))
          result))
      (catch TimeoutException e
        (util/println-error "\nExecution Time Out! Your code execution time took more than 2.5 seconds."
                            (get-test-failed-msg test-var route e))
        false)
      (catch SecurityException e
        (util/println-error "\nYou can not use one of the blacklisted Java Classes & Clojure Namespaces/Symbols!"
                            (get-test-failed-msg test-var route e))
        false)
      (catch Throwable e
        (util/println-error (get-test-failed-msg test-var route e))
        false)
      (finally
        (-> sb-ns-prefix symbol remove-ns)
        (-> sb-helper-ns-prefix symbol remove-ns)))
    false))