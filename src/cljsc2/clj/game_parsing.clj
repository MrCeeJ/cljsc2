(ns cljsc2.clj.game-parsing
  (:require [cljsc2.clj.core :as core]
            [manifold.stream :as s :refer [stream]]
            [clojure.data :refer [diff]]
            [datascript.core :as ds]
            [datascript.transit :refer [read-transit-str write-transit-str]]
            [taoensso.nippy :as nippy]
            [clojure.spec.alpha :as spec]
            ))

(def schema
  {:unit/type {:db/cardinality :db.cardinality/many
               :db/valueType :db.type/ref}
   :unit/buff-ids {:db/cardinality :db.cardinality/many
                   :db/valueType :db.type/ref}
   :unit-type/abilities {:db/cardinality :db.cardinality/many
                         :db/valueType :db.type/ref}
   :upgrade-type/ability-id {:db/cardinality :db.cardinality/many
                             :db/valueType :db.type/ref}})

(def knowledge-base
  (read-transit-str
   (nippy/thaw-from-file "resources/static-terran-transit-str.nippy")))

(def upgrade-keymap
  {:upgrade-id :upgrade-type/id
   :name :upgrade-type/name
   :ability-id :upgrade-type/ability-id
   :research-time :upgrade-type/research-time
   :mineral-cost :upgrade-type/mineral-cost
   :vespene-cost :upgrade-type/vespene-cost})

(def unit-type-keymap
  {:movement-speed :unit-type/movement-speed
   :weapons :unit-type/weapons
   :food-provided :unit-type/food-provided
   :race :unit-type/race
   :name :unit-type/name
   :ability-id :unit-type/ability-id
   :build-time :unit-type/build-time
   :unit-id :unit-type/unit-id
   :sight-range :unit-type/sight-range
   :tech-alias :unit-type/tech-alias
   :armor :unit-type/armor
   :available :unit-type/available
   :attributes :unit-type/attributes
   :mineral-cost :unit-type/mineral-cost
   :vespene-cost :unit-type/vespene-cost})

(def ability-type-keymap
  {:allow-minimap :ability-type/allow-minimap
   :is-instant-placement :ability-type/is-instant-placement
   :is-building :ability-type/is-building
   :ability-id :ability-type/id
   :allow-autocast :ability-type/allow-autocast
   :footprint-radius :ability-type/footprint-radius
   :remaps-to-ability-id :ability-type/remaps-to-ability-id
   :available :ability-type/available
   :target :ability-type/target
   :friendly-name :ability-type/name
   :cast-range :ability-type/cast-range})

(def unit-keymap
  {:add-on-tag :unit/add-on-tag
   :orders :unit/orders
   :passengers :unit/passengers
   :is-blip :unit/is-blip
   :assigned-harvesters :unit/assigned-harvesters
   :unit-type :unit/unit-type
   :shield :unit/shield
   :is-selected :unit/is-selected
   :health-max :unit/health-max
   :weapon-cooldown :unit/weapon-cooldown
   :facing :unit/facing
   :is-powered :unit/is-powered
   :is-on-screen :unit/is-on-screen
   :pos :unit/pos
   :ideal-harvesters :unit/ideal-harvesters
   :energy :unit/energy
   :radius :unit/radius
   :build-progress :unit/build-progress
   :cloak :unit/cloak
   :cargo-space-taken :unit/cargo-space-taken
   :alliance :unit/alliance
   :detect-range :unit/detect-range
   :is-burrowed :unit/is-burrowed
   :display-type :unit/display-type
   :vespene-contents :unit/vespene-contents
   :energy-max :unit/energy-max
   :radar-range :unit/radar-range
   :health :unit/health
   :buff-ids :unit/buff-ids
   :cargo-space-max :unit/cargo-space-max
   :tag :unit/tag
   :is-flying :unit/is-flying
   :shield-max :unit/shield-max
   :owner :unit/owner
   })

(defn obs->facts [{:keys [game-loop player-common raw-data]}]
   (let [{:keys [minerals vespene food-cap food-used food-workers
                 idle-worker-count army-count player-id food-cap]} player-common
         game-loop-id (+ 1234000000 game-loop)]
     (concat [{:db/id game-loop-id
               :player-common/minerals minerals
               :player-common/vespene vespene
               :player-common/food-used food-used
               :player-common/food-cap food-cap
               :player-common/food-workers food-workers
               :player-common/idle-worker-count idle-worker-count
               :player-common/army-count army-count}
              {:db/id -1 :meta/latest-game-loop game-loop}
              {:db/id -2 :meta/player-id player-id}]
             (map
              (fn [unit]
                (merge (clojure.set/rename-keys
                       (select-keys
                        unit
                        (vals unit-keymap))
                       unit-keymap)
                       {:db/id (:tag unit)
                        :unit/x (:x (:pos unit))
                        :unit/y (:y (:pos unit))
                        :unit/z (:z (:pos unit))
                        :unit/buff-ids (map #(+ 660000 %) (:buff-ids unit))}))
              (:units raw-data)))
     ))

(defn distance [x1 y1 x2 y2]
  (let [dx (- x2 x1), dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn ability-to-action
  ([tags ability]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags tags
       :ability-id ability
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand{}}}}})
  ([tags ability target-unit-tag]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags tags
       :ability-id ability
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand
       {:target-unit-tag target-unit-tag}}}}})
  ([tags ability x y]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags tags
       :ability-id ability
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand
       {:target-world-space-pos #:SC2APIProtocol.common$Point2D
        {:x x
         :y y}}}}}}))

(defn can-place? [conn ability-id builder-tag x y]
  (identical? (-> (core/send-request-and-get-response-message
                   conn
                   #:SC2APIProtocol.query$RequestQuery
                   {:query #:SC2APIProtocol.query$RequestQuery
                    {:placements
                     [#:SC2APIProtocol.query$RequestQueryBuildingPlacement
                      {:ability-id ability-id
                       :placing-unit-tag builder-tag
                       :target-pos #:SC2APIProtocol.common$Point2D
                       {:x x
                        :y y}}]}})
                  :query :placements first :result)
              :success))

(defn find-location [connection builder-tag ability-id positions]
  (loop [remaining-positions positions
         [x y] (first positions)]
    (let [can-place? (can-place? connection ability-id builder-tag x y)]
      (cond
        can-place? [[x y] remaining-positions]
        (not (empty? remaining-positions)) (recur (rest remaining-positions)
                                                  (first remaining-positions))
        :else [false (rest remaining-positions)]))))

(defn positions-around
  ([initial-x initial-y in-radius]
   (positions-around initial-x initial-y in-radius 1))
  ([initial-x initial-y in-radius step]
   (for [x (take in-radius
                 (interleave (range 0 10) (reverse (range -10 0 step))))
         y (take in-radius
                 (interleave (range 0 10) (reverse (range -10 0 step))))]
     [(+ initial-x x) (+ initial-y y)])))

(defn build-scvs [latest-knowledge _]
  (->> (ds/q '[:find ?unit-tag ?build-ability
               :where
               [?build-me :unit-type/name "SCV"]
               [?build-me :unit-type/ability-id ?build-ability]
               [(+ 880000 ?build-ability) ?ability-e-id]
               [?builder-type-id :unit-type/abilities ?ability-e-id]
               [?builder-type-id :unit-type/unit-id ?unit-id]
               [?unit-tag :unit/unit-type ?unit-id]
               ]
             latest-knowledge)
       (map (fn [[unit-tag ability-id]]
              (ability-to-action [unit-tag] ability-id)))))

(defn build-marines [latest-knowledge _]
  (->> (ds/q '[:find ?unit-tag ?build-ability
               :where
               [?build-me :unit-type/name "Marine"]
               [?build-me :unit-type/ability-id ?build-ability]
               [(+ 880000 ?build-ability) ?ability-e-id]
               [?builder-type-id :unit-type/abilities ?ability-e-id]
               [?builder-type-id :unit-type/unit-id ?unit-id]
               [?unit-tag :unit/unit-type ?unit-id]
               ]
             latest-knowledge)
       (map (fn [[unit-tag ability-id]]
              (ability-to-action [unit-tag] ability-id)))))

(defn build-supply-depots [latest-knowledge connection]
  (let [food-available (first (ds/q '[:find [?food-available]
                                      :where
                                      [?l :player-common/food-used ?food-used]
                                      [?l :player-common/food-cap ?food-cap]
                                      [(- ?food-cap ?food-used) ?food-available]]
                                    latest-knowledge))
        build-new-supply-depots (int (/ (- 13 food-available) 10))
        [_ ability-id] (ds/q '[:find [?unit-tag ?build-ability]
                               :where
                               [?build-me :unit-type/name "SupplyDepot"]
                               [?build-me :unit-type/ability-id ?build-ability]
                               [(+ 880000 ?build-ability) ?ability-e-id]
                               [?builder-type-id :unit-type/abilities ?ability-e-id]
                               [?builder-type-id :unit-type/unit-id ?unit-id]
                               [?unit-tag :unit/unit-type ?unit-id]
                               ]
                             latest-knowledge)
        builders (ds/q '[:find [?builder-tag ...]
                         :where
                         [?builder-tag :unit/unit-type ?type-id]
                         [?type-e-id :unit-type/unit-id ?type-id]
                         [?type-e-id :unit-type/name "SCV"]
                         ]
                       latest-knowledge)
        ordered (ds/q '[:find ?unit-tag ?orders
                        :where
                        [?unit-tag :unit/orders ?orders]]
                      latest-knowledge)
        building-already (->> ordered
                              (map (fn [[unit-tag orders]]
                                     [unit-tag (count (filter (comp #{ability-id}
                                                                    :ability-id) orders))]))
                              (filter (fn [[_ building-count]]
                                        (> 0 building-count))))
        [near-x near-y] (ds/q '[:find [?x ?y]
                                :where
                                [?id :unit/unit-type ?type-id]
                                [(+ 990000 ?type-id) ?type-e-id]
                                [?type-e-id :unit-type/name "CommandCenter"]
                                [?id :unit/x ?x]
                                [?id :unit/y ?y]]
                              latest-knowledge)
        positions (positions-around (+ near-x 7) near-y 10)
        ]
    (get (reduce
          (fn [{:keys [actions positions]} unit-tag]
            (let [[position remaining-pos] (find-location connection unit-tag ability-id positions)]
              (if position
                {:actions (conj actions (ability-to-action [unit-tag] ability-id (first position) (second position)))
                 :positions remaining-pos}
                (reduced {:actions actions
                          :positions remaining-pos}))))
          {:positions positions
           :actions []}
          (take (inc
                 (- build-new-supply-depots
                    (count building-already)))
                builders))
         :actions)))


(defn build-barracks [latest-knowledge connection]
  (let [[builder-tag ability-id] (ds/q '[:find [?unit-tag ?build-ability]
                                         :where
                                         [?build-me :unit-type/name "Barracks"]
                                         [?build-me :unit-type/ability-id ?build-ability]
                                         [(+ 880000 ?build-ability) ?ability-e-id]
                                         [?builder-type-id :unit-type/abilities ?ability-e-id]
                                         [?builder-type-id :unit-type/unit-id ?unit-id]
                                         [?unit-tag :unit/unit-type ?unit-id]
                                         ]
                                       latest-knowledge)
        ordered (ds/q '[:find ?unit-tag ?orders
                        :where
                        [?unit-tag :unit/orders ?orders]]
                      latest-knowledge)
        building-already (->> ordered
                              (map (fn [[unit-tag orders]]
                                     [unit-tag (count (filter (comp #{ability-id}
                                                                    :ability-id) orders))]))
                              (filter (fn [[_ building-count]]
                                        (> 0 building-count))))
        [near-x near-y] (ds/q '[:find [?x ?y]
                                :where
                                [?id :unit/unit-type ?type-id]
                                [(+ 990000 ?type-id) ?type-e-id]
                                [?type-e-id :unit-type/name "CommandCenter"]
                                [?id :unit/x ?x]
                                [?id :unit/y ?y]]
                              latest-knowledge)
        currently-building-count (count building-already)
        positions (positions-around (+ 5 near-x) near-y 10)
        existing-barrack-count (count (ds/q '[:find ?unit-id
                                              :where
                                              [?id :unit-type/name "Barracks"]
                                              [?id :unit-type/unit-id ?unit-type-id]
                                              [?unit-id :unit/unit-type ?unit-type-id]]
                                            latest-knowledge))
        builders (ds/q '[:find [?builder-tag ...]
                         :where
                         [?builder-tag :unit/unit-type ?type-id]
                         [?type-e-id :unit-type/unit-id ?type-id]
                         [?type-e-id :unit-type/name "SCV"]
                         ]
                       latest-knowledge)
        ]
    (get (reduce
          (fn [{:keys [actions positions]} unit-tag]
            (let [[position remaining-pos] (find-location connection unit-tag
                                                          ability-id positions)]
              (if position
                {:actions (conj actions
                                (ability-to-action [unit-tag] ability-id
                                                   (first position)
                                                   (second position)))
                 :positions remaining-pos}
                (reduced {:actions actions
                          :positions remaining-pos}))))
          {:positions positions
           :actions []}
          (take (- 5
                   existing-barrack-count)
                builders))
         :actions)))

(defn attack-with [first-gather-amount unit-name x y]
    (fn [latest-knowledge _]
      (if (>= (count (ds/q '[:find ?tag
                           :in $ ?unit-name
                           :where
                           [?tag :unit/unit-type ?type-id]
                           [?type :unit-type/unit-id ?type-id]
                           [?type :unit-type/name ?unit-name]]
                         latest-knowledge
                         unit-name))
              first-gather-amount
              )
        [#:SC2APIProtocol.sc2api$Action
         {:action-ui #:SC2APIProtocol.ui$ActionUI
          {:action #:SC2APIProtocol.ui$ActionUI{:select-army {}}}}
         #:SC2APIProtocol.sc2api$Action
         {:action-raw #:SC2APIProtocol.raw$ActionRaw
          {:action #:SC2APIProtocol.raw$ActionRaw
           {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
            {:target #:SC2APIProtocol.raw$ActionRawUnitCommand
             {:target-world-space-pos #:SC2APIProtocol.common$Point2D
              {:x x :y y}}
             :ability-id 23}}}}]
        []))
    )

(def strategies
  [build-scvs
   build-supply-depots
   build-barracks
   build-marines
   (attack-with 40 "Marine" 40 50)
   #_(attack-with 40 "Marine" 25 20)])

(defn create-actions [knowledge-base strategies obs connection]
  (let [latest-knowledge (ds/db-with knowledge-base
                                     (obs->facts obs))]
    (mapcat (fn [strategy]
              (strategy latest-knowledge connection))
            strategies)))

(comment
  (def sc-process (core/start-client))
  (def conn (core/restart-conn))

  (core/load-simple-map conn)
  (core/do-sc2
   conn
   (fn [observation connection]
     (def obs observation)
     (create-actions knowledge-base strategies observation connection))
   {:run-until-fn (core/run-for 5000)}))
