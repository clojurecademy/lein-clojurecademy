(ns leiningen.clojurecademy
  (:refer-clojure :exclude [test])
  (:require [leiningen.help :refer [help-for subtask-help-for]]
            [leiningen.clojurecademy.autotest :refer [autotest test]]
            [leiningen.clojurecademy.deploy :refer [deploy]]
            [leiningen.clojurecademy.blacklist :refer [blacklist]]
            [leiningen.clojurecademy.course :refer [course]]
            [clojurecademy.dsl.validator :as validator]
            [clojure.tools.namespace.find :as ii]
            [colorize.core :as cc]
            [leiningen.clojurecademy.util :as util])
  (:import (java.io File Console)))

(defn- load-src-nses
  [project]
  (doseq [nss (util/get-nsses-under-src project)]
    (require nss :reload)))

(defn- ensure-clojurecademy-map-set!
  [project]
  (when-not (-> project :clojurecademy :course-map)
    (util/println-error (str "Missing :clojurecademy option in project.clj.\n"
                             "You need to have a line in your project.clj file that looks like:\n"
                             "  :clojurecademy {:course-map your.ns/course-map}"))
    (System/exit 1)))

(defn- nary? [v n]
  (some #{n} (map count (:arglists (meta v)))))

(defn clojurecademy
  "Test & Deploy Clojurecademy courses with ease."
  {:help-arglists '([test autotest deploy blacklist course])
   :subtasks      [#'test #'autotest #'deploy #'blacklist #'course]}
  ([project]
   (println (if (nary? #'help-for 2)
              (help-for project "clojurecademy")
              (help-for "clojurecademy"))))
  ([project subtask & args]
   (ensure-clojurecademy-map-set! project)
   (load-src-nses project)
   (case subtask
     "test" (apply test project args)
     "autotest" (apply autotest project args)
     "deploy" (apply deploy project args)
     "blacklist" (apply blacklist project args)
     "course" (apply course project args)
     (util/println-warn "Subtask: " (str \" subtask \") " not found." (subtask-help-for *ns* #'clojurecademy)))))