(ns leiningen.clojurecademy.blacklist
  (:require [leiningen.help :refer [help-for subtask-help-for]]
            [leiningen.clojurecademy.sandbox :as s]
            [leiningen.clojurecademy.util :as util]
            [leinjacker.eval :as jacker]))

(defn- print-blacklist-namespaces
  []
  (util/print-console "Blacklisted Namespaces" (s/blacklist-nses)))

(defn- print-blacklist-packages
  []
  (util/print-console "Blacklisted Java Packages" (s/blacklist-packages)))

(defn- print-blacklist-symbols
  []
  (util/print-console "Blacklisted Symbols" s/blacklist-symbols))

(defn- print-blacklist-objects
  []
  (util/print-console "Blacklisted Objects" s/blacklist-objects))

(defn- list-all-blacklisted
  []
  (print-blacklist-namespaces)
  (print-blacklist-packages)
  (print-blacklist-symbols)
  (print-blacklist-objects))

(defn- blacklist-task
  [project type]
  (jacker/eval-in-project project
                          `(do
                             ~(condp contains? type
                                #{nil} (list-all-blacklisted)
                                #{"namespace" "-n"} (print-blacklist-namespaces)
                                #{"package" "-p"} (print-blacklist-packages)
                                #{"symbol" "-s"} (print-blacklist-symbols)
                                #{"object" "-o"} (print-blacklist-objects)
                                (util/println-warn "Subtask: " (str \" type \") " not found." (subtask-help-for *ns* #'blacklist-task))))))

(defn blacklist
  ([project]
   (blacklist-task project nil))
  ([project type]
   (blacklist-task project type)))