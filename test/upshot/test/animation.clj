(ns upshot.test.animation
  (:use [upshot.animation])
  (:use [midje.sweet]))

(defn check-instance? [expected] #(instance? expected %))

;*******************************************************************************

(facts "about path-transition"
  (let [pt (path-transition :orientation :orthogonal-to-tangent
                            :cycle-count :indefinite)]
    (class pt) => javafx.animation.PathTransition))

