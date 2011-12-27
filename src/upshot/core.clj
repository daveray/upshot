;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.core
  (:use [seesaw.options :only [option-provider option-map apply-options
                               default-option OptionProvider
                               get-option-value
                               apply-options]])
  (:use [seesaw.config :only [Configurable config!* config*]])
  (:use [seesaw.util :only [illegal-argument]])
  (:require [clojure.string]
            [seesaw.selector]
            [seesaw.util]))

;*******************************************************************************

(extend-protocol seesaw.selector/Selectable
  javafx.scene.Node
    (id-of* [this] (if-let [id (.getId this)] (keyword id)))
    (id-of!* [this id] (.setId this (if id (name id))) this)
    (class-of* [this] (set (.getStyleClass this)))
    (class-of!* [this classes] 
      (-> this 
        .getStyleClass 
        (.setAll (map name (if (coll? classes) classes [classes]))))))

(extend-protocol seesaw.util/Children
  javafx.scene.Parent
    (children [this] (seq (.getChildrenUnmodifiable this))))

;*******************************************************************************

; A hack to force initialization of the JavaFX toolkit. Otherwise, you
; can't do anything outside of an Application sub-class which isn't fun
; for the REPL.
; TODO do something else.
(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

(defn run-later* 
  [f]
  (javafx.application.Platform/runLater f))

(defmacro run-later
  [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now* 
  [f]
  (let [result (promise)]
    (run-later
      (deliver result (try (f) (catch Throwable e e))))
    @result))

(defmacro run-now
  [& body]
  `(run-now* (fn [] ~@body)))
;*******************************************************************************

(defn- paint-handler [v]
  (cond
    (instance? javafx.scene.paint.Color) v
    (instance? String v) (javafx.scene.paint.Color/web ^String v)
    (keyword? v) (javafx.scene.paint.Color/web ^String (name v))
    (vector? v)
      (let [[a b c d] v
            n         (count v)]
        (case n
          2 (javafx.scene.paint.Color/web (name a) b)
          3 (javafx.scene.paint.Color/color a b c)
          (javafx.scene.paint.Color/color a b c d)))))

;*******************************************************************************

(defn- dash-case
  [^String s]
  (let [gsub (fn [s re sub] (.replaceAll (re-matcher re s) sub))]
    (-> s
      (gsub #"([A-Z]+)([A-Z][a-z])" "$1-$2")
      (gsub #"([a-z]+)([A-Z])" "$1-$2")
      (clojure.string/lower-case)))) 

(declare to-event-handler)
(defn- get-option-info [m]
  (if (and (= 1 (count (.getParameterTypes m)))
          (.matches (.getName m) "^set[A-Z].*"))
    (let [base-name (.substring (.getName m) 3)
          type      (first (.getParameterTypes m))
          dash-name (dash-case base-name)]
      { :setter (symbol  (.getName m))
        :getter (symbol  (str "get" base-name))
        :name   (keyword (if (= Boolean/TYPE type)
                           (str dash-name "?")
                           dash-name))
        :event (if (= javafx.event.EventHandler type)
                  type)
        :type   type
        :paint (= javafx.scene.paint.Paint type)
        :enum   (.getEnumConstants type) })))

(defmacro options-for-class [class]
  `(option-map
     ~@(for [{:keys [setter getter name event type enum paint]} (->> (resolve class)
              .getDeclaredMethods
              (remove #(.isSynthetic %))
              (filter #(let [ms (.getModifiers %)]
                         (= java.lang.reflect.Modifier/PUBLIC 
                            (bit-and ms
                                     (bit-or java.lang.reflect.Modifier/PUBLIC
                                             java.lang.reflect.Modifier/STATIC)))))
              (map get-option-info)
              (filter identity))]
         (cond
           event `(default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter (to-event-handler v#))))
                      (fn [c#] (.. c# ~getter))
                      ["(fn [event] ...)"])
           paint `(default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter (paint-handler v#))))
                      (fn [c#] (.. c# ~getter))
                      [:white [:white 0.5] [1 1 1] [1 1 1 0.5]])
           enum `(let [set-conv# ~(into {} (for [e enum]
                                             [(keyword (dash-case (.name e)))
                                              (symbol (.getName type) (.name e)) ]))
                       get-conv# (clojure.set/map-invert set-conv#)] 
                   (default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter (set-conv# v#))))
                      (fn [c#]    (get-conv# (.. c# ~getter)))
                     (keys set-conv#)))
           :else `(default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter v#)))
                      (fn [c#] (.. c# ~getter))
                      [~type])))))

;*******************************************************************************

(extend-protocol Configurable
  javafx.stage.Stage
    (config* [this name] (get-option-value this name))
    (config!* [this args] (apply-options this args))
  javafx.scene.Scene
    (config* [this name] (get-option-value this name))
    (config!* [this args] (apply-options this args))
  javafx.scene.Node 
    (config* [this name] (get-option-value this name))
    (config!* [this args] (apply-options this args)))

(def ^{:doc (str "Alias of seesaw.config/config:\n" (:doc (meta #'seesaw.config/config)))} config seesaw.config/config)

(def ^{:doc (str "Alias of seesaw.config/config!:\n" (:doc (meta #'seesaw.config/config!)))} config! seesaw.config/config!)

(defn event-handler* 
  [f] 
  (reify javafx.event.EventHandler 
    (handle [this e] (f e))))

(defmacro event-handler [arg & body]
  `(event-handler* (fn ~arg ~@body)))

(defn to-event-handler [v]
  (cond
    (instance? javafx.event.EventHandler v) v
    (fn? v) (event-handler* v)
    :else (illegal-argument "Don't know how to make event-handler from %s" v)))

;*******************************************************************************

(def stage-options (options-for-class javafx.stage.Stage))
(option-provider javafx.stage.Stage stage-options)
(defn stage [& opts] (apply-options (javafx.stage.Stage.) opts))

;*******************************************************************************

(def scene-options (options-for-class javafx.scene.Scene))
(option-provider javafx.scene.Scene scene-options)
(defn scene [& {:keys [root width height] 
                :or {width 0.0 height 0.0} 
                :as opts}] 
  (apply-options (javafx.scene.Scene. root width height) 
                 (dissoc opts :root :width :height)))

;*******************************************************************************

(def node-options 
  (merge
    (options-for-class javafx.scene.Node)
    (option-map
      (default-option :id
        seesaw.selector/id-of!
        seesaw.selector/id-of
        "A keyword id")
      (default-option :class
        seesaw.selector/class-of!
        seesaw.selector/class-of
        "A keyword class or set of keywords"))))

;*******************************************************************************

(def group-options
  (merge
    node-options
    (option-map
      (default-option :children
        (fn [g v]
          (-> g .getChildren (.setAll v)))
        (fn [g] (-> g .getChildren))))))

(option-provider javafx.scene.Group group-options)

(defn group [& opts] (apply-options (javafx.scene.Group.) opts))

;*******************************************************************************
(def shape-options
  (merge
    node-options
    (options-for-class javafx.scene.shape.Shape)))

;*******************************************************************************

(def circle-options
  (merge
    shape-options
    (options-for-class javafx.scene.shape.Circle)))

(option-provider javafx.scene.shape.Circle circle-options)

(defn circle 
  [& opts] 
  (apply-options (javafx.scene.shape.Circle.) opts))

;*******************************************************************************


(comment
  (run-now 
    (doto 
      (stage 
        :scene (scene 
                 :root (group
                         :children (for [i (range 30)]
                                     (circle :radius (* 10 i) 
                                             :center-x (* 10 i)
                                             :center-y (* 10 i)
                                             :fill     [:white 0.05]
                                             :stroke-type :outside 
                                             :stroke  [:white 0.16]
                                             :stroke-width 4)))
                 :width 800.0 :height 600.0 
                 :fill :black))
      .show)))

