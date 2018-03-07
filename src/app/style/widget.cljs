
(ns app.style.widget (:require [hsl.core :refer [hsl]]))

(def button
  {:color "white",
   :line-height 2,
   :background-color (hsl 200 90 60),
   :display "inline-block",
   :padding "0 8px"})

(def global {:font-family "Verdana"})
