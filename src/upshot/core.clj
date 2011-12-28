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
  (:use [seesaw.util :only [check-args illegal-argument]])
  (:require [clojure.string]
            [seesaw.selector]
            [seesaw.selection]
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
  javafx.scene.Node
    (children [this] nil)
  javafx.scene.Parent
    (children [this] (seq (.getChildrenUnmodifiable this))))

; TOOD make this real
(defn- to-node [v]
  (cond
    (instance? javafx.scene.Node v) v
    (instance? javafx.scene.Scene v) (.getRoot v)
    (instance? javafx.stage.Stage v) (-> v .getScene .getRoot)))

(defn select
  ([root selector]
    (check-args (vector? selector) "selector must be vector")
    (let [root (to-node root)
          result (seesaw.selector/select root selector)
          id? (and (nil? (second selector)) (seesaw.selector/id-selector? (first selector)))]

      (if id? (first result) result))))

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
    (instance? javafx.scene.paint.Paint) v
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

; A macro to handle most of the boilerplate for each kind of object
(defmacro defobject [func-name class base-options extra-options] 
  (let [opts-name (symbol (str (name func-name) "-options"))]
    `(do 
       (def ~opts-name 
         (merge
           ~@base-options
           (options-for-class ~class)
           ~@extra-options))

       (option-provider ~class ~opts-name)

       (defn ~func-name 
         [& opts#] 
         (apply-options (new ~class) opts#))
       ))) 

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
(def ^:private children-option
  (default-option :children
    (fn [g v]
      (-> g .getChildren (.setAll v)))
    (fn [g] (-> g .getChildren))
    ["A seq of nodes"]))

(defobject group javafx.scene.Group 
  [node-options]
  [(option-map children-option)])

;*******************************************************************************

(def region-options
  (merge
    node-options
    (options-for-class javafx.scene.layout.Region)))

(defobject pane javafx.scene.layout.Pane [region-options] 
  [(option-map children-option)])

(defobject border-pane javafx.scene.layout.BorderPane [pane-options] [])
(defobject flow-pane javafx.scene.layout.FlowPane [pane-options] [])
(defobject h-box javafx.scene.layout.HBox [pane-options] [])
(defobject v-box javafx.scene.layout.VBox [pane-options] [])
(defobject tile-pane javafx.scene.layout.TilePane [pane-options] [])

;*******************************************************************************

(defobject web-view javafx.scene.web.WebView [node-options] 
  [(option-map
     (default-option
       :url
       (fn [v url] (-> v .getEngine (.load url)))
       (fn [v] (-> v .getEngine .getLocation))
       "Displayed URL as a string")
     (default-option :engine
       nil
       (fn [v] (.getEngine v))))])

;*******************************************************************************

(def shape-options
  (merge
    node-options
    (options-for-class javafx.scene.shape.Shape)))

(defobject circle javafx.scene.shape.Circle [shape-options] [])
(defobject rectangle javafx.scene.shape.Rectangle [shape-options] [])
(defobject arc javafx.scene.shape.Arc [shape-options] [])
(defobject ellipse javafx.scene.shape.Ellipse [shape-options] [])
(defobject line javafx.scene.shape.Line [shape-options] [])
(defobject svg-path javafx.scene.shape.SVGPath [shape-options] [])

;*******************************************************************************

(def control-options
  (merge
    node-options
    (options-for-class javafx.scene.control.Control)))

(defobject accordion javafx.scene.control.Accordion [control-options] 
  [(option-map
      (default-option :panes
        (fn [g v]
          (-> g 
            .getPanes 
            (.setAll v)))
        (fn [g] (-> g .getPanes))
        "seq of (titled-pane)"))])

(defobject choice-box javafx.scene.control.ChoiceBox [control-options] 
  [(option-map
      (default-option :items
        (fn [g v]
          (-> g 
            .getItems 
            (.setAll v)))
        (fn [g] (-> g .getItems))
        "seq of items to choose from"))])

(def labeled-options
  (merge
    control-options
    (options-for-class javafx.scene.control.Labeled)))

(defobject label javafx.scene.control.Label [labeled-options] [])
(defobject titled-pane javafx.scene.control.TitledPane [labeled-options] [])

(def button-base-options
  (merge
    labeled-options 
    (options-for-class javafx.scene.control.ButtonBase)))

(defobject button javafx.scene.control.Button [button-base-options] [])
(defobject check-box javafx.scene.control.CheckBox [button-base-options] [])
(defobject hyperlink javafx.scene.control.Hyperlink [button-base-options] [])
(defobject menu-button javafx.scene.control.MenuButton [button-base-options] [])
(defobject button javafx.scene.control.Button [button-base-options] [])
(defobject toggle-button javafx.scene.control.ToggleButton [button-base-options] [])

(def text-input-control-options 
  (merge
    control-options
    (options-for-class javafx.scene.control.TextInputControl)))

(defobject text-field javafx.scene.control.TextField [text-input-control-options] [])
(defobject password-field javafx.scene.control.PasswordField [text-field-options] [])
(defobject text-area javafx.scene.control.TextArea [text-input-control-options] [])

(defobject html-editor javafx.scene.web.HTMLEditor [control-options] [])

;*******************************************************************************
(extend-protocol seesaw.selection/Selection
  javafx.scene.control.SingleSelectionModel
    (get-selection [this] [(.getSelectedItem this)])
    (set-selection [this [v]]
      (if v
        (.select this v)
        (.clearSelection this)))

  javafx.scene.control.ChoiceBox
    (get-selection [this] (seesaw.selection/get-selection (.getSelectionModel this)))
    (set-selection [this args] (seesaw.selection/set-selection (.getSelectionModel this) args))
  
  
  )

(defn selection
  ([target] (selection target {}))
  ([target options] 
   (seesaw.selection/selection target options)))

(defn selection!
  ([target new-selection] (selection! target {} new-selection))
  ([target opts new-selection] (seesaw.selection/selection! target opts new-selection)))

;*******************************************************************************

; TODO Value stuff
 
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
