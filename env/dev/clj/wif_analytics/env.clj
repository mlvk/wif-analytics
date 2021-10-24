(ns wif-analytics.env
  (:require
   [selmer.parser :as parser]
   [clojure.tools.logging :as log]
   [wif-analytics.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[wif-analytics started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[wif-analytics has shut down successfully]=-"))
   :middleware wrap-dev})
