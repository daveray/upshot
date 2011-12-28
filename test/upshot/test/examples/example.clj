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
               (key-frame 0 
                 (key-value (property ball :translate-x) 0)
                 (key-value (property ball :translate-y) 0)
                 (key-value (property ball :fill) (to-paint :red)))
               (key-frame 1 
                 (key-value (property ball :translate-x) 200)
                 (key-value (property ball :translate-y) 200)
                 (key-value (property ball :fill) (to-paint :blue)))
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
