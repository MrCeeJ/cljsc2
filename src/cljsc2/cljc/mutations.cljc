(ns cljsc2.cljc.mutations
  (:require [cljsc2.cljs.ui.run :refer [RunConfig]]
            [cljsc2.cljs.ui.player_setup :refer [PlayerSetup]]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [cljsc2.cljs.ui.resolution :refer [Resolution InterfaceOptions]]
            [fulcro.ui.form-state :as fs]
            [datascript.core :as ds]))

(defmutation edit-run-config [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (fs/add-form-config* RunConfig [:run-config/by-id id])
                             (assoc-in [:run-config/by-id id :ui/editting] true))))))

(defmutation abort-run-config [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (fs/pristine->entity* [:run-config/by-id id])
                             (assoc-in [:run-config/by-id id :ui/editting] false)
                             )))))

(defmutation submit-run-config [{:keys [id delta]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (assoc-in [:run-config/by-id id :ui/editting] false)
                             (fs/entity->pristine* [:run-config/by-id id])
                             ))))
  (remote [env] true))

(defmutation send-request [_]
  (remote [env] true))

(defmutation send-action [_]
  (remote [env] true))

(defmutation edit-player-setup [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/add-form-config* PlayerSetup [:player-setup/by-id id])
                       (assoc-in [:player-setup/by-id id :ui/editting] true))))))

(defmutation abort-player-setup [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/pristine->entity* [:player-setup/by-id id])
                       (assoc-in [:player-setup/by-id id :ui/editting] false))))))

(defmutation submit-player-setup [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (assoc-in [:player-setup/by-id id :ui/editting] false)
                       (fs/entity->pristine* [:player-setup/by-id id])))))
  (remote [env] true))


(defmutation edit-resolution [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/add-form-config* Resolution [:resolution/by-id id])
                       (assoc-in [:resolution/by-id id :ui/editting] true))))))

(defmutation abort-resolution [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/pristine->entity* [:resolution/by-id id])
                       (assoc-in [:resolution/by-id id :ui/editting] false))))))

(defmutation submit-resolution [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (assoc-in [:resolution/by-id id :ui/editting] false)
                       (fs/entity->pristine* [:resolution/by-id id])))))
  (remote [env] true))

(defmutation edit-interface-options [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/add-form-config* InterfaceOptions [:interface-options/by-id id])
                       (assoc-in [:interface-options/by-id id :ui/editting] true))))))

(defmutation abort-interface-options [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/pristine->entity* [:interface-options/by-id id])
                       (assoc-in [:interface-options/by-id id :ui/editting] false))))))

(defmutation submit-interface-options [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (assoc-in [:interface-options/by-id id :ui/editting] false)
                       (fs/entity->pristine* [:interface-options/by-id id])))))
  (remote [env] true))

(defmutation make-savepoint [{:keys [port game-loop]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (assoc-in [:process/by-id port :process/savepoint-at]
                                       game-loop)))))
  (remote [env] true))

(defmutation load-savepoint [{:keys [port]}]
  (remote [env] true))

(defmutation make-conn [_]
  (value [{:keys [state]}]
         (let [knowledge-base (:root/starcraft-static-data @state)]
            (swap! state assoc :root/starcraft-static-data
                   (ds/conn-from-datoms
                    (:eavt knowledge-base)
                    (:schema knowledge-base))))))

(defmutation update-map [{:keys [id path]}]
  (action [{:keys [state]}]
          (swap! state assoc-in [:map-config/by-id id :map-config/path] path))
  (remote [env] true))

(defmutation update-player-setup [{:keys [id field value]}]
  (action [{:keys [state]}]
          (swap! state assoc-in [:player-setup/by-id id field] value)))
