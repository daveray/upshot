;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.core
  (:use [seesaw.options :only [option-provider
                               option-map
                               default-option
                               get-option-value
                               apply-options]])
  (:use [seesaw.config :only [Configurable config!* config*]])
  (:use [seesaw.util :only [check-args illegal-argument cond-doto]])
  (:use [upshot.options :only [options-for-class defobject]])
  (:require [clojure.set]
            [seesaw.selector]
            [seesaw.selection]
            [upshot.events]
            [upshot.paint]))

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
(defn to-node [v]
  (cond
    (instance? javafx.event.Event v) (to-node (.getSource v))
    (instance? javafx.scene.Node v) v
    (instance? javafx.scene.Scene v) (.getRoot v)
    (instance? javafx.stage.Stage v) (-> v .getScene .getRoot)))

(defn to-scene [v] (-> v to-node .getScene))

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


;*******************************************************************************

(def window-options (options-for-class javafx.stage.Window))
#_(def stage-options
  (merge
    window-options
    (options-for-class javafx.stage.Stage)))

(defobject stage javafx.stage.Stage [window-options] [])

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
        #(->> % seesaw.selector/class-of (map keyword) set)
        "A keyword class or set of keywords")
      (default-option :parent
        nil
        #(.getParent ^javafx.scene.Node %)
        "javafx.scene.Parent (read-only)")
      (default-option :scene
        nil
        #(.getScene ^javafx.scene.Node %)
        "javafx.scene.Scene (read-only)"))))

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
(defobject stack-pane javafx.scene.layout.StackPane [pane-options] [])

(defn anchors! [^javafx.scene.Node n & {:keys [top bottom left right]}]
  (when top    (javafx.scene.layout.AnchorPane/setTopAnchor n (double top)))
  (when bottom (javafx.scene.layout.AnchorPane/setBottomAnchor n (double bottom)))
  (when left   (javafx.scene.layout.AnchorPane/setLeftAnchor n (double left)))
  (when right  (javafx.scene.layout.AnchorPane/setRightAnchor n (double right)))
  n)

(defn anchors [^javafx.scene.Node n]
  { :top    (javafx.scene.layout.AnchorPane/getTopAnchor n)
    :bottom (javafx.scene.layout.AnchorPane/getBottomAnchor n)
    :left   (javafx.scene.layout.AnchorPane/getLeftAnchor n)
    :right  (javafx.scene.layout.AnchorPane/getRightAnchor n)
  })

(defobject anchor-pane javafx.scene.layout.AnchorPane
  [pane-options]
  [])

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
(defobject text javafx.scene.text.Text [shape-options] [])

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

