;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.test.examples.line-chart
  (:use [upshot.core]))

(def data
  {:series-a [[0 0] [1 1] [2 4] [3 9]]
   :series-b [[0 9] [1 4] [2 1] [3 0]]})

(defn make-scene []
  (scene
    :root (border-pane
            :center (line-chart
                      :id :chart
                      :data data))
    :width 800.0
    :height 600.0
    :fill :black))

(defn add-behaviors [root]
  root)

(defn run []
  (run-now
    (-> (stage :scene (make-scene))
      add-behaviors
      .show)))
(defn -main [& args]
  (run))
;(run)

