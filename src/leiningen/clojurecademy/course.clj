(ns leiningen.clojurecademy.course
  (:require [leiningen.help :refer [help-for subtask-help-for]]
            [leiningen.clojurecademy.util :as util]
            [leiningen.clojurecademy.autotest :as a]
            [leinjacker.eval :as jacker]))

(defn- route
  [project]
  (let [course-map (-> project :clojurecademy :course-map resolve deref)
        route-vecs (->> course-map
                        a/get-routes
                        (map :route-vec))]
    (util/print-console "All Routes" route-vecs)
    (println "You have" (count route-vecs) "routes.")))

(defn- deftest
  [project]
  (let [test-vars (map (fn [test-var]
                         (str (-> test-var meta :name) " -> " (:route-vec @test-var)))
                       (a/get-test-vars project))]
    (util/print-console "All Deftests" test-vars)
    (println "You have" (count test-vars) "deftests.")))

(defn course
  ([project]
   (course project nil))
  ([project type]
   (jacker/eval-in-project project
                           `(do
                              ~(condp contains? type
                                 #{nil} (do (route project) (deftest project))
                                 #{"route" "-r"} (route project)
                                 #{"test" "-t"} (deftest project)
                                 (util/println-warn "Subtask: " (str \" type \") " not found." (subtask-help-for *ns* #'course)))))))