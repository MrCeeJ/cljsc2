(ns cljsc2.cljs.actions
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [react :as react]
            [clojure.core.async :refer [<! >!]]
            [cljsc2.cljs.core :refer [render-canvas feature-layer-draw-descriptions]]
            [cljsc2.cljc.model :as model]
            [cljs.spec.alpha :as spec]
            [fulcro.client :as fc]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.ui.form-state :as fs]
            [fulcro.client.data-fetch :refer [load] :as df]
            [fulcro.websockets :as fw]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [fulcro.websockets.networking :refer [push-received]]
            [fulcro.ui.bootstrap3 :as bs]
            [datascript.core :as ds]))

(defn ui-available-actions [available-abilities knowledge-base
                            selected-ability action-select]
  (apply
   dom/div
   (map (fn [[id ability-name requires-point]]
                  (dom/button #js {:key id
                                   :onClick (fn [s] (action-select id ability-name requires-point))
                                   :disabled (= id (:ability-id selected-ability))}
                              (str ability-name " " requires-point)))
                (ds/q '[:find ?ability-id ?ability-name
                        :in $ [?ability-id ...]
                        :where
                        [?e :ability-type/id ?ability-id]
                        [?e :ability-type/name ?ability-name]]
                      @knowledge-base
                      (map :ability-id available-abilities)))))
