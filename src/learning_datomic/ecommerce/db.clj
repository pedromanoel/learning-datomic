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
             {:db/ident       :product/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/doc         "Unique id for the product"
              :db/unique      :db.unique/identity}

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

(def schema-with-tag
  (conj schema
        {:db/ident       :product/tag
         :db/valueType   :db.type/string
         :db/cardinality :db.cardinality/many
         :db/doc         "Tags for the product"}))

(def schema-with-category
  (conj schema
        {:db/ident       :product/category
         :db/valueType   :db.type/ref
         :db/cardinality :db.cardinality/one
         :db/doc         "The category of the product"}

        {:db/ident       :category/name
         :db/valueType   :db.type/string
         :db/cardinality :db.cardinality/one
         :db/doc         "Name of a category"}

        {:db/ident       :category/id
         :db/valueType   :db.type/uuid
         :db/cardinality :db.cardinality/one
         :db/doc         "UUID of the category"
         :db/unique      :db.unique/identity}))

(defn create-schema
  [conn]
  (d/transact conn schema))

(defn all-product-entity-ids [db]
  (d/q '{:find  [?e]
         :where [[?e :product/name]]}
       db))

(defn all-products-with-path [db path]
  (d/q '{:find  [?e]
         :in    [$ ?path]
         :where [[?e :product/path ?path]]}
       db
       path))

(defn all-paths [db]
  (d/q '{:find  [?path]
         :where [[_ :product/path ?path]]}
       db))

(defn all-prices [db]
  (d/q '{:find  [?price]
         :where [[_ :product/price ?price]]}
       db))

(defn product-names-and-prices-cartesian [db]
  (d/q '{:find  [?name ?price]
         :where [[_ :product/name ?name]
                 [_ :product/price ?price]]}
       db))

(defn product-names-and-prices [db]
  (d/q '{:find  [?name ?price]
         :keys  [product/name product/price]
         :where [[?e :product/name ?name]
                 [?e :product/price ?price]]}
       db))

(defn all-products-with-all-attrs
  "Only work when entity have all attributes set"
  [db]
  (d/q '{:find  [?e ?name ?path ?price]
         :keys  [product/id product/name product/path product/price]
         :where [[?e :product/name ?name]
                 [?e :product/path ?path]
                 [?e :product/price ?price]]}
       db))

(defn all-products
  "Works with sparse entities"
  [db]
  (d/q '{:find  [(pull ?e [:product/name :product/path :product/price])]
         :where [[?e :product/name ?name]]}
       db))

(defn all-products-*
  "Works with sparse entities"
  [db]
  (d/q '{:find  [(pull ?e [*])]
         :where [[?e :product/name ?name]]}
       db))

(defn all-categories
  [db]
  (d/q '{:find  [(pull ?e [*])]
         :where [[?e :category/id]]}
       db))

(defn all-products-with-prices-greater-than [db minimum-price]
  (d/q '{:find  [(pull ?e [*])]
         :in    [$ ?minimum-price]
         :where [[?e :product/price ?price]
                 [(> ?price ?minimum-price)]]}
       db minimum-price))

(defn find-product-by-eid [db id]
  (d/pull db '[*] id))

(defn find-product-by-product-id [db product-id]
  ;; with lookup ref
  (d/pull db '[*] [:product/id product-id]))

(defn find-product-by-name [db name]
  (d/q '{:find  [(pull ?e [*])]
         :in    [$ ?name]
         :where [[?e :product/name ?name]]}
       db name))

(defn first-product-with-path [db path]
  (ffirst (d/q '{:find  [(pull ?e [*])]
                 :in    [$ ?path]
                 :where [[?e :product/path ?path]]}
               db path)))

(defn uuid->eid [db id-attr uuid]
  (ffirst (d/q '{:find  [?e]
                 :in    [$ ?id-attr ?uuid]
                 :where [[?e ?id-attr ?uuid]]}
               db id-attr uuid)))

(defn lookup-ref [attribute entity]
  [attribute (attribute entity)])

(defn reduce-adds-for-category [category]
  (fn [adds product]
    (let [product-ref (lookup-ref :product/id product)
          category-ref (lookup-ref :category/id category)
          new-add [:db/add product-ref :product/category category-ref]]
      (conj adds new-add))))

(defn db-adds-to-associate-product-and-category
  [products category]
  (reduce (reduce-adds-for-category category) [] products))

(defn attribute-category-to-products!
  [conn products category]
  (d/transact conn (db-adds-to-associate-product-and-category products category)))

(defn save! [conn entities]
  (d/transact conn entities))

(defn create-schema! [conn]
  (d/transact conn schema-with-category))