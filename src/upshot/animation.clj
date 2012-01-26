;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.animation
  (:use [upshot.options :only [options-for-class]]
        [upshot.events :only [to-event-handler]]
        [seesaw.options :only [apply-options option-provider]])
  (:require [clojure.set]
            [seesaw.util]))

;*******************************************************************************

(defmacro property
  [this prop-name]
  (let [getter (-> prop-name name seesaw.util/camelize (str "Property") symbol)]
    `(. ~this ~getter)))

(defn key-value
  ([target end] (javafx.animation.KeyValue. target end))
  ([target end interp] (javafx.animation.KeyValue. target end interp)))

(defn to-key-value
  [v]
  (cond
    (instance? javafx.animation.KeyValue v) v
    :else (apply key-value v)))

;*******************************************************************************

(defn to-duration
  [v]
  (cond
    (instance? javafx.util.Duration v) v)
    :else (javafx.util.Duration/seconds (double v)))

(defn key-frame
  [& args]
  (let [values (take-while (complement keyword) args)
        {:keys [time on-finished]}   (apply hash-map (drop-while (complement keyword) args))]
    (javafx.animation.KeyFrame.
      (to-duration (or time 0.0))
      (to-event-handler (or on-finished (fn [_])))
      (into-array javafx.animation.KeyValue (map to-key-value values)))))

(defn to-key-frame
  [v]
  (cond
    (instance? javafx.animation.KeyFrame v) v
    :else (apply key-frame v)))

;*******************************************************************************
(def animation-options (options-for-class javafx.animation.Animation))

(def timeline-options
  (merge
    animation-options
    (options-for-class javafx.animation.Timeline)))

(option-provider javafx.animation.Timeline timeline-options)

(defn timeline
  [& args]
  (let [frames (take-while (complement keyword) args)
        opts   (drop-while (complement keyword) args)]
    (apply-options
      (javafx.animation.Timeline.
        (into-array
          javafx.animation.KeyFrame
          (map to-key-frame frames)))
      opts)))

(defn play!
  [^javafx.animation.Animation a]
  (.play a)
  a)

(defn stop!
  [^javafx.animation.Timeline tl]
  (.stop tl)
  tl)

