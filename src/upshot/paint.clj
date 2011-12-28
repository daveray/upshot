;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.paint)

(defn ^javafx.scene.paint.Paint to-paint [v]
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
