(ns learning-datomic.ecommerce.db
  (:require [datomic.api :as d]))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

;; imperative language because these functions have side effects
(defn connect-to-db
  []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn erase-db
  []
  (d/delete-database db-uri))

;; there are no tables in datomic, but there are datoms
;; entity           attribute           value         transaction    add?
;; 213761287361     :product/name       "Computer"    tx1            true
;; 213761287361     :product/path       "/computer"   tx2            true
;; 213761287361     :product/price      2500.10       tx3            true

(def schema [
             {:db/ident       :product/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Name of the product"}

             {:db/ident       :product/path
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "The path of the product"}

             {:db/ident       :product/price
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/doc         "The path of the product"}])

(def new-schema (conj schema
                      {:db/ident       :product/tag
                       :db/valueType   :db.type/string
                       :db/cardinality :db.cardinality/many
                       :db/doc         "Tags for the product"}))

(defn create-schema
  [conn]
  (d/transact conn schema))

(defn all-product-entity-ids [db]
  (d/q '[:find ?e
         :where [?e :product/name]]
       db))

(defn all-products-with-path [db path]
  (d/q '[:find ?e
         :in $ ?path
         :where [?e :product/path ?path]]
       db
       path))

(defn all-paths [db]
  (d/q '[:find ?path
         :where [_ :product/path ?path]]
       db))

(defn all-prices [db]
  (d/q '[:find ?price
         :where [_ :product/price ?price]]
       db))

(defn product-names-and-prices-cartesian [db]
  (d/q '[:find ?name ?price
         :where [_ :product/name ?name]
                [_ :product/price ?price]]
       db))

(defn product-names-and-prices [db]
  (d/q '[:find ?name ?price
         :keys product/name product/price
         :where [?e :product/name ?name]
                [?e :product/price ?price]]
       db))

(defn all-products-with-all-attrs
  "Only work when entity have all attributes set"
  [db]
  (d/q '[:find ?e ?name ?path ?price
         :keys product/id product/name product/path product/price
         :where [?e :product/name ?name]
                [?e :product/path ?path]
                [?e :product/price ?price]] db))

(defn all-products
  "Works with sparse entities"
  [db]
  (d/q '[:find (pull ?e [:product/name :product/path :product/price])
         :where [?e :product/name ?name]] db))

(defn all-products-*
  "Works with sparse entities"
  [db]
  (d/q '[:find (pull ?e [*])
         :where [?e :product/name ?name]] db))

(defn all-products-with-prices-greater-than [db minimum-price]
  (d/q '[:find (pull ?e [*])
         :in $ ?minimum-price
         :where [?e :product/price ?price]
                [(> ?price ?minimum-price)]] db minimum-price))

(defn find-product [db id]
  (d/entity db id))

(defn first-product-with-path [db path]
  (first (first (d/q '[:find (pull ?e [*])
                       :in $ ?path
                       :where [?e :product/path ?path]]
                     db path)))
  )