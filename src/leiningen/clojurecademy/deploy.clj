(ns leiningen.clojurecademy.deploy
  (:require [leiningen.clojurecademy.autotest :as test]
            [leiningen.clojurecademy.repl.helper-fns :as helper-fns]
            [leinjacker.eval :as jacker]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [kezban.core :refer :all])
  (:import (java.io File)))


(defn- get-cred-from-console
  []
  (println "Once you set your credentials in ~/.lein/profiles.clj you don't have to enter everytime.\nSuch as:\n{:user {:clojurecademy {:username-or-email \"your-username-or-email\"\n                        :password          \"your-password\"}}}")
  (print "Username or e-mail: ") (flush)
  (let [username-or-email (read-line)
        console           (System/console)
        password          (if console
                            (.readPassword console "%s" (into-array ["Password: "]))
                            (do
                              (println "LEIN IS UNABLE TO TURN OFF ECHOING, SO" "THE PASSWORD IS PRINTED TO THE CONSOLE")
                              (print "Password: ")
                              (flush)
                              (read-line)))]
    {:username-or-email username-or-email :password (apply str (seq password))}))


(defn- get-credentials
  [project]
  (if-let* [username-or-email (-> project :clojurecademy :username-or-email)
            password (-> project :clojurecademy :password)]
           {:username-or-email username-or-email :password password}
           (get-cred-from-console)))


(defn- write-course-to-a-file
  [file course-map helper-fns]
  (with-open [w (io/writer file)]
    (.write w (str "[ " course-map "\n\n" helper-fns " ]"))))


(defn- print-deploying
  [flag]
  (print "Deploying...") (flush)
  (future (while @flag
            (print ".") (flush)
            (Thread/sleep 500))))


(defn- post!
  [creds file]
  (client/post "http://localhost:3000/course/upload"
               {:headers   {"username-or-email" (:username-or-email creds) "password" (:password creds)}
                :multipart [{:name "Content/type" :content "plain/text"}
                            {:name "file" :content (clojure.java.io/file file)}]})
  (println "\nSuccessfully Deployed"))


(defn- upload-course
  [creds course-map helper-fns]
  (let [file (File/createTempFile "clojurecademy-map" ".txt")
        flag (atom true)]
    (write-course-to-a-file file course-map helper-fns)
    (print-deploying flag)
    (try
      (post! creds file)
      (catch Exception e
        (println " -> " (or (-> e Throwable->map :data :body) "Host is not reachable"))
        (println "Course could not be deployed!"))
      (finally
        (reset! flag false)))))


(defn deploy
  [project]
  (jacker/eval-in-project project
                          `(do ~(when (test/test project)
                                  (let [course-map    (-> project :clojurecademy :course-map resolve deref str)
                                        helper-fns-ns (-> project :clojurecademy :helper-fns-ns)
                                        helper-fns    (-> helper-fns-ns test/get-helper-fns helper-fns/get-helper-fns-source str)
                                        creds         (get-credentials project)
                                        content       (str "[ " course-map "\n\n" helper-fns " ]")]
                                    (if (> (count (.getBytes content "UTF-8")) 11000000)
                                      (println "File is too big! You can not upload more than 10MB")
                                      (upload-course creds course-map helper-fns)))))))