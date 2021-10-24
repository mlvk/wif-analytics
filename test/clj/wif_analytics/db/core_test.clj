(ns wif-analytics.db.core-test
  (:require [clojure.test :refer :all]
            [java-time.pre-java8]
            [mount.core :as mount]
            [wif-analytics.config :refer [env]]
            [wif-analytics.db.core :as db]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'wif-analytics.config/env
     #'wif-analytics.db.core/conn)

    (f)))