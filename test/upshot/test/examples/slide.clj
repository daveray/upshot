;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.test.examples.slide
  (:use [upshot.core]
        [upshot.animation]
        [upshot.paint]))

; Adapted from http://saidandem.blogspot.com/2012/01/sliding-in-javafx-its-all-about.html
(defn make-action-pane []
  (h-box :alignment :center
         :spacing 10
         :pref-height 40
         :children [(button :id :slide-up :text "Slide Up")
                    (button :id :slide-down :text "Slide Down")]))

(defn make-box []
  (let [bottom (stack-pane
                 :id :bottom
                 :children [(rectangle :width 200 :height 250
                                       :fill :yellow
                                       )
                            (text :text "Click the above \"Slide Down\" button to see the top pane content..."
                                  :wrapping-width 200
                                  ; TODO :font
                                  )])
        top (stack-pane
              :id :top
              :children [(rectangle :width 200 :height 250
                                       :fill :green
                                       )
                         (stack-pane
                           :children [(text :text "Click the below \"Slide Up\" button to see the bottom pane content..."
                                            :wrapping-width 200
                                            ; TODO :font
                                            )])])
        container (stack-pane :pref-width 200 :pref-height 250
                              :style "-fx-border-width:1px;-fx-border-style:solid;-fx-border-color:#999999;"
                             :children [bottom top])]
    (v-box
      :alignment :center
      :children [(make-action-pane)
                 (group :children [container])])))

(defn make-scene []
  (scene
    :root (doto (make-box)
            .autosize)))

(defn make-stage []
  (stage
    :title "Slide Effect Demo"
    :width 350
    :height 300
    :scene (make-scene)))

(defn add-animation [root]
  (let [top-pane (select root [:#top])
        bottom-pane (select root [:#bottom])

        clip-rect (rectangle :height 0 :width 200
                             :translate-y 250)
        timeline-bounce (timeline
                          (key-frame
                            (key-value (property clip-rect :height) (- 250 15))
                            (key-value (property clip-rect :translate-y) 15)
                            (key-value (property top-pane :translate-y) -15)
                            :time 0.100)
                          :cycle-count 2
                          :auto-reverse? true)
        on-finished (fn [e] (play! timeline-bounce))
        timeline-down (timeline
                        (key-frame
                          (key-value (property clip-rect :height) 250)
                          (key-value (property clip-rect :translate-y) 0)
                          (key-value (property top-pane :translate-y) 0)
                          :time 0.200
                          :on-finished on-finished)
                        :cycle-count 1
                        :auto-reverse? true)
        timeline-up (timeline
                      (key-frame
                        (key-value (property clip-rect :height) 0)
                        (key-value (property clip-rect :translate-y) 250)
                        (key-value (property top-pane :translate-y) -250)
                        :time 0.200)
                      :cycle-count 1
                      :auto-reverse? true)]
    (config! top-pane
             :clip clip-rect
             :translate-y -250)

    {:timeline-up timeline-up :timeline-down timeline-down}))

(defn add-behaviors [root]
  (let [slide-up (select root [:#slide-up])
        slide-down (select root [:#slide-down])
        {:keys [timeline-up timeline-down]} (add-animation root)]
    (config! slide-up :on-action (fn [e] (play! timeline-up )))
    (config! slide-down :on-action (fn [e] (play! timeline-down))))

  root)

(defn run []
  (run-now
    (-> (make-stage)
      add-behaviors
      .show)))

(defn -main [& args]
  (run))

;(run)

