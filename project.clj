(defproject net.aaroniba/ring-webutils "1.0.0"
  :description "Various utilities for making a webserver in ring."
  :url "https://github.com/aiba/ring-webutils"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring "1.4.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [ns-tracker "0.3.0"]
                 [clj-logging-config/clj-logging-config "1.9.12"]
                 [org.clojure/tools.logging "0.3.1"]]
  :source-paths ["src"]
  :profiles {:dev {:source-paths ["src" "src-test"]
                   :dependencies []}}
  :target-path "target/%s/")
