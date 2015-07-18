(ns net.aaroniba.ring-webutils.server
  (:require [clojure.tools.logging :as log]
            [net.aaroniba.ring-webutils.core :as wu]))

(wu/config-logging! "./logs/ring-webutils.log")

(defn render [req]
  (wu/plaintext-response (pr-str req)))

(defn gen-handler []
  (-> render
      wu/wrap-params
      wu/wrap-reload
      wu/wrap-exceptions))

(defonce server* (wu/webserver))

(defn restart! []
  (wu/restart-webserver! server* 8000 (gen-handler)))

(comment
  (restart!)
  )
