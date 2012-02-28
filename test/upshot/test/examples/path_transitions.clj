;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.test.examples.path-transitions
  (:use [upshot.core]
        [upshot.animation]))

; Adapted from http://java.dzone.com/articles/javafx-2-animation-path

(defn generate-curvy-path [opacity]
  (path :id :curvy-path
        :opacity opacity
        :elements [(move-to :x 20 :y 20)
                   (cubic-curve-to :control-x1 380 :control-y1 0 :control-x2 380 :control-y2 120 :x 200 :y 120)
                   (cubic-curve-to :control-x1 0 :control-y1 120 :control-x2 0 :control-y2 240 :x 380 :y 240)]))

(defn determine-path-opacity [] 1.0)

(defn make-scene []
  (scene
    :width 600
    :height 400
    :fill javafx.scene.paint.Color/GHOSTWHITE
    :root (group :children [(generate-curvy-path (determine-path-opacity))
                            (circle :id :circle
                                    :center-x 20 :center-y 20 :radius 15
                                    :fill javafx.scene.paint.Color/DARKRED)
                            (circle :center-x 20 :center-y 20 :radius 5)
                            (circle :center-x 380 :center-y 240 :radius 5)
                            ])))

(defn make-stage []
  (stage
    :title "Path Transitions"
    :scene (make-scene)))

(defn generate-path-transition
  [shape path]
  (path-transition
    :duration (javafx.util.Duration/seconds 8.0)
    :delay (javafx.util.Duration/seconds 2.0)
    :path path
    :node shape
    :orientation :orthogonal-to-tangent
    :cycle-count :indefinite
    :auto-reverse? true))

(defn apply-animation
  [root]
  (let [c (select root [:#circle])
        p (select root [:#curvy-path])
        t (generate-path-transition c p)]
    (play! t)))

(defn run []
  (run-now
    (let [sc (make-scene)
          st (stage :title "Path Transitions" :scene sc)]
      (.show st)
      (apply-animation sc))))

(defn -main [& args]
  (run))

;(run)

