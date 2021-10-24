(ns wif-analytics.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[wif-analytics started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[wif-analytics has shut down successfully]=-"))
   :middleware identity})
