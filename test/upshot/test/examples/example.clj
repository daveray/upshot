;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.test.examples.example
  (:use [upshot.core]
        [upshot.animation]
        [upshot.paint]))

(defn make-scene []
  (scene
    :root (border-pane
            :top (h-box
                   :children [(button :id :play :text "Play")
                              (button :id :stop :text "Stop")])
            :center (circle :id :ball :radius 50
                            :fill :white))
    :width 800.0 :height 600.0
    :fill :black))

(defn add-behaviors [root]
  (let [ball (select root [:#ball])
        play (select root [:#play])
        stop (select root [:#stop])
        tl   (timeline
               (key-frame
                 (key-value (property ball :translate-x) 0)
                 (key-value (property ball :translate-y) 0)
                 (key-value (property ball :fill) (to-paint :red))
                 :time 0.0)
               (key-frame
                 (key-value (property ball :translate-x) 200)
                 (key-value (property ball :translate-y) 200)
                 (key-value (property ball :fill) (to-paint :blue))
                 :time 1.0)
               :auto-reverse? true
               :cycle-count  5)]
    (config! play :on-action (fn [e] (play! tl)))
    (config! stop :on-action (fn [e] (stop! tl))))
  root)

(defn run []
  (run-now
    (-> (stage :scene (make-scene))
      add-behaviors
      .show)))
(defn -main [& args]
  (run))
;(run)

