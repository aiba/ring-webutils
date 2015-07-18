(ns net.aaroniba.ring-webutils.core
  (:require [clojure.tools.logging :as log]
            [clj-logging-config.log4j :refer [set-loggers!]]
            [hiccup.core :as hiccup]
            [hiccup.page :refer [doctype]]
            [ring.util.response :as response]
            [ns-tracker.core :refer [ns-tracker]]
            [clojure.java.io :as io]
            [org.httpkit.server :as httpserver]
            (ring.middleware params keyword-params multipart-params))
  (:import org.apache.commons.lang3.exception.ExceptionUtils))

;; Logging —————————————————————————————————————————————————————————————————————

(def ^:private logconfig-lock* (Object.))
(def ^:private logconfig-syskey (str ::logging-configured?))

(defn- appender [f]
  (org.apache.log4j.FileAppender.
   (org.apache.log4j.PatternLayout. "%d{ISO8601} %-5p %c | %m%n")
   f
   true))

(defn config-logging! [outf]
  (locking logconfig-lock*
    (when-not (= (System/getProperty logconfig-syskey) "true")
      (let [f (io/file outf)]
        (.mkdirs (.getParentFile f))
        (set-loggers! :config {:level :info}
                      :root {:level :info
                             :out (appender outf)})
        (System/setProperty logconfig-syskey "true")
        (println "logging to" outf)))))

;; Caching —————————————————————————————————————————————————————————————————————

(defn- never-cache-headers []
  {"Expires" "Sat, 18 Jun 1983 07:07:07 GMT"
   "Last-Modified" (.toGMTString (java.util.Date.))
   "Cache-Control" (str "no-store, no-cache, must-revalidate, "
                        "post-check=0, pre-check=0")
   "Pragma" "no-cache"})

(defn- always-cache-headers []
  {"Expires" "Sat, 18 Jun 2017 07:07:07 GMT"
   "Cache-Control", "max-age=31536000"})

(defn never-cache [r]
  (assoc r
         :headers (merge (:headers r)
                         (never-cache-headers))))

(defn always-cache [r]
  (assoc r :headers (merge (r :headers) (always-cache-headers))))

(defn wrap-never-cache [handler]
  (fn [req]
    (let [r (handler req)]
      (never-cache r))))

;; Rendering hiccup html ———————————————————————————————————————————————————————

(defn hiccup-to-str [& elts]
  (hiccup/html (doctype :html5) (list* elts)))

(defn hiccup-html-response [& elements]
  (-> elements
      (hiccup-to-str)
      (response/response)
      (response/content-type "text/html")
      (response/charset "utf-8")))

(defn plaintext-response [s]
  (-> s
      (response/response)
      (response/content-type "text/plain")
      (response/charset "utf-8")))

;; Middleware ——————————————————————————————————————————————————————————————————

(defn wrap-reload [handler & [options]]
  (let [source-dirs (get options :dirs ["src"])
        modified-namespaces (ns-tracker source-dirs)
        q (atom [])]
    (fn [request]
      (swap! q #(distinct (concat % (modified-namespaces))))
      (doseq [ns-sym @q]
        (require ns-sym :reload)
        (swap! q #(remove #{ns-sym} %)))
      (handler request))))

(defn wrap-exceptions [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable ex
        (let [s (ExceptionUtils/getStackTrace ex)]
          (log/error (str "\n" (apply str (repeat 80 "-")) "\n" s "\n\n"))
          (assoc (plaintext-response s)
                 :status 500))))))

(defn wrap-params [handler]
  (-> handler
      (ring.middleware.keyword-params/wrap-keyword-params)
      (ring.middleware.params/wrap-params)
      (ring.middleware.multipart-params/wrap-multipart-params)))

;; Making an actual webserver ——————————————————————————————————————————————————

(defn webserver []
  (atom nil))

(defn restart-webserver! [server port handler]
  (swap! server
         (fn [s]
           (when s (s))
           (httpserver/run-server handler {:port port})))
  (log/info "Webserver on port" port))
