(ns leiningen.clojurecademy.autotest
  (:refer-clojure :exclude [test])
  (:require [leiningen.clojurecademy.util :as util]
            [leiningen.clojurecademy.repl.runner :as rr]
            [leiningen.clojurecademy.conf :as conf]
            [leinjacker.eval :as jacker]
            [clojurecademy.dsl.validator :as validator]
            [clojure.string :as str]
            [clojure.tools.nrepl :as repl]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [clojure.pprint :refer [pprint]]
            [clojail.core :refer [sandbox]]
            [clojail.testers :as testers]
            [kezban.core :refer :all]
            [ns-tracker.core :as ns-track])
  (:import (clojure.lang RT)
           (org.apache.commons.io IOUtils)
           (java.util.concurrent TimeUnit Executors)))

(def modified-namespaces (ns-track/ns-tracker ["src"]))

(defn get-routes
  [course-map]
  (flatten
    (for [chapter         (:chapters course-map)
          sub-chapter     (:sub-chapters chapter)
          subject         (:subjects sub-chapter)
          sub-instruction (-> subject :instruction :sub-instructions)]
      {:route-vec (->> [chapter sub-chapter subject (:instruction subject) sub-instruction]
                       (map :name)
                       vec)
       :subject   subject})))

(defn get-test-vars
  [project]
  (->> (util/get-nsses-under-src project)
       (map ns-publics)
       (mapcat vals)
       (filter validator/test?)))

(defn- find-test-var
  [route test-vars]
  (some #(when (= (:route-vec route) (:route-vec (deref %))) %) test-vars))

(defn- print-not-found-tests-vars
  [not-found-test-vars]
  (when (seq not-found-test-vars)
    (util/println-error "\n-Test vars could not be found-")
    (util/println-error (apply str (map #(str % "\n") not-found-test-vars)))))

(defn- print-passed-and-failed-routes
  [test-results]
  (util/println-success (count (filter true? test-results)) " routes passed.")
  (when-let* [false-test-results-count (count (filter false? test-results))
              _ (not= false-test-results-count 0)]
             (util/println-error (count (filter false? test-results)) " routes failed.")))

(defn- collect-not-found-routes
  [route test-var not-found-test-vars]
  (if test-var
    not-found-test-vars
    (conj not-found-test-vars (:route-vec route))))

(defn- collect-test-results
  [route test-var test-results helper-fns]
  (conj test-results (rr/eval-test route test-var helper-fns)))

(defn- verified-cmd?
  [debug-mode debug-advanced]
  (cond
    (and debug-mode (not (contains? #{"debug" "-d"} debug-mode)))
    (util/println-warn (format "Debug mode: %s is not found.Usage is:->\nlein clojurecademy autotest debug(or -d)\n" debug-mode))

    (and debug-advanced (not (contains? #{"advanced" "-a"} debug-advanced)))
    (util/println-warn (format "Debug advanced: %s is not found.Usage is:->\nlein clojurecademy autotest debug advanced(or -a)\n" debug-advanced))

    :else
    true))

(defn- set-debug!
  [debug-mode debug-advanced]
  (when debug-mode
    (alter-var-root (var conf/debug-mode) (constantly true))
    (when debug-advanced
      (alter-var-root (var conf/debug-advanced) (constantly true)))))

(defn- run-tests
  [routes test-vars helper-fns]
  (loop [[route & other-routes :as r] routes
         test-results        []
         not-found-test-vars []]
    (if (seq r)
      (let [test-var (find-test-var route test-vars)]
        (recur other-routes
               (collect-test-results route test-var test-results helper-fns)
               (collect-not-found-routes route test-var not-found-test-vars)))
      (do
        (print-not-found-tests-vars not-found-test-vars)
        (print-passed-and-failed-routes test-results)
        (every? true? test-results)))))

(defn get-helper-fns
  [ns]
  (when ns
    (require ns)
    (some->> ns
             ns-publics
             vals
             first
             meta
             :file
             (.getResourceAsStream (RT/baseLoader))
             IOUtils/toString)))


(defn get-clj-version
  [project]
  (some (fn [[pckg version & _]]
          (when (= pckg 'org.clojure/clojure)
            version)) (-> project :dependencies)))


(defn- check-clj-version
  [project]
  (when-not (= "1.8.0" (get-clj-version project))
    (util/println-error "Your Clojure version is not valid! Please use 1.8.0")
    (System/exit 0)))


(defn test
  ([project]
   (test project nil nil))
  ([project debug-mode]
   (test project debug-mode nil))
  ([project debug-mode debug-advanced]
   (when (verified-cmd? debug-mode debug-advanced)
     (check-clj-version project)
     (set-debug! debug-mode debug-advanced)
     (let [course-map    (-> project :clojurecademy :course-map resolve deref)
           helper-fns-ns (-> project :clojurecademy :helper-fns-ns)
           helper-fns    (get-helper-fns helper-fns-ns)]
       (when (util/valid-course-map? course-map)
         (let [test-vars (get-test-vars project)
               routes    (get-routes course-map)]
           (if (seq test-vars)
             (run-tests routes test-vars helper-fns)
             (util/println-warn "There is no test var defined.Please add `defcoursetest` for testing."))))))))


(defn- schedule-auto-check
  [auto]
  (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1)
                        #(when-let [m (seq (modified-namespaces))]
                           (doseq [ns-sym m]
                             (try
                               (require ns-sym :reload)
                               (catch Exception e
                                 (util/println-error "Could not reload ns: " ns-sym " - Please restart autotest!")
                                 (System/exit 0))))
                           (auto)) 0 500 TimeUnit/MILLISECONDS))


(defn autotest
  ([project]
   (autotest project nil nil))
  ([project debug-mode]
   (autotest project debug-mode nil))
  ([project debug-mode debug-advanced]
   (jacker/eval-in-project project
                           `(do
                              ~(let [auto #(test project debug-mode debug-advanced)]
                                 (schedule-auto-check auto)
                                 (auto)
                                 @(promise))))))