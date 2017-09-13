(defproject lein-clojurecademy "0.1.0"

  :description "Clojurecademy Leiningen Plugin"

  :url "https://github.com/clojurecademy/lein-clojurecademy"

  :author "Ertuğrul Çetin"

  :license {:name "Apache License"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [clojurecademy/dsl "0.3.5"]
                 [clj-http "3.6.1"]
                 [colorize "0.1.1" :exclusions [org.clojure/clojure]]
                 [leinjacker "0.4.2"]
                 [uochan/watchtower "0.1.4"]
                 [ns-tracker "0.3.1"]
                 [kezban "0.1.7"]
                 [clojail "1.0.6"]]

  :eval-in-leiningen true

  :jvm-opts ["-server"
             "-Djava.security.policy=example.policy"
             "-XX:+UseConcMarkSweepGC"
             "-XX:+CMSParallelRemarkEnabled"
             "-XX:+UseCMSInitiatingOccupancyOnly"
             "-XX:CMSInitiatingOccupancyFraction=70"
             "-XX:+ScavengeBeforeFullGC"
             "-XX:+CMSScavengeBeforeRemark"]

  :uberjar-name "lein-clojurecademy.jar")