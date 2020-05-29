(ns learning-datomic.ecommerce.core
  (:require [clojure.pprint :refer [pprint]])
  (:require [datomic.api :as d]
            (learning-datomic.ecommerce
              [db :as db]
              [model :as model]))
  (:import (java.util Date)))

(defn h [title]
  (str "--- " title " ---"))

(defn group [title & values]
  (cons (h title) values))

(def computer-id (model/uuid))
(def electronics-id (model/uuid))

(defn get-fresh-database-conn []
  (db/erase-db)
  (let
    [conn (db/connect-to-db)]
    (db/create-schema conn)
    conn))

(defn seed-database [conn]
  (let
    [computer (model/new-product computer-id "Computer" "/computer" 2500.10M)
     phone (model/new-product "Phone" "/phone" 888.88M)
     calculator {:product/name "Calculator"}
     tv (model/new-product "TV" "/tv" 33000.00M)
     product-nil-val {:product/path nil}
     product-bad-path {:product/paths "/path"}]
    #_(d/transact conn [product-nil-val])                   ; would fail because value is nil
    #_(d/transact conn [product-bad-path])                  ; would fail because attribute is undeclared
    (pprint (d/transact conn [computer phone calculator]))  ; atomic operation
    (let [derefd-transaction @(d/transact conn [tv])
          {:keys [db-before db-after tx-data tempids]} derefd-transaction
          [tx-datom] tx-data
          entity-id (-> tempids
                        vals
                        first)]
      (pprint db-before)
      (pprint db-after)
      (pprint tx-data)
      (pprint tx-datom)
      (pprint (type tx-datom))
      (pprint tempids)
      (pprint entity-id)
      (pprint @(d/transact conn [[:db/add entity-id :product/price 30000.00M]]))
      (pprint @(d/transact conn [[:db/retract entity-id :product/price]]))))
  conn)

(defn run-query-examples
  [conn]
  (group
    "query examples"
    (group
      "Query example"
      (d/q '[:find ?e
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (group
      "Query all paths"
      (db/all-paths (d/db conn)))

    (group
      "Query all prices"
      (db/all-prices (d/db conn)))

    (group
      "Query example with implicit :in"
      (d/q '[:find ?e
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (group
      "Query example with explicit :in"
      (d/q '[:find ?e
             :in $
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (group
      "Query example with parameters"
      (d/q '[:find ?e ?path
             :in $ ?path
             :where [?e :product/path ?path]]
           (d/db conn)
           "/computer"))

    (group
      "Query example with keys"
      (d/q '[:find ?e ?path
             :keys product/id product/path
             :where [?e :product/path ?path]]
           (d/db conn)))

    (group
      "Select everything (does not work because there are no bindings)"
      #_(d/q '[:find ?e ?a ?v ?t
               :where [?e ?a ?v ?t]]
             (d/db conn)))

    (group
      "Select schemas"
      (d/q '[:find (pull ?e [*])
             :where [?e :db/ident]]
           (d/db conn)))))

(defn run-db-examples
  [conn]
  (let [db (d/db conn)]
    (group
      "db examples"
      (group "Query with params" (db/all-product-entity-ids db))
      (group
        "Query with params"
        (db/all-products-with-path db "/computer")
        (db/all-products-with-path db "/tv")
        (db/all-products-with-path db "/dont-exist"))
      (group "Cartesian names and prices" (db/product-names-and-prices-cartesian db))
      (group "Same entities names and prices" (db/product-names-and-prices db))
      (group "All products with all attrs" (db/all-products-with-all-attrs db))
      (group "All products" (db/all-products db))
      (group "All products with all attrs" (db/all-products-* db))
      (group "With predicates" (db/all-products-with-prices-greater-than db 1000M))
      (group "Product by eid" (db/find-product-by-eid db (ffirst (db/all-product-entity-ids db))))
      (group "Product by product id" (db/find-product-by-product-id db computer-id))
      (group "Product by name" (db/find-product-by-name db "Computer"))
      #_(group "Transact same uuid" (d/transact conn)))))

(defn run-time-examples
  [conn]
  (let [inst-1 (Date.)
        db-1 (d/db conn)
        chair (model/new-product "Chair" "/chair" 199.99M)
        mouse (model/new-product "Mouse" "/mouse" 25.00M)
        {db-2 :db-after} @(d/transact conn [chair mouse])
        inst-2 (Date.)]
    (group
      "time examples"
      (group
        "add first two products"
        (db/all-products db-1))
      (group
        "after adding two more products"
        (db/all-products db-2))
      (group
        "using as-of at instant 1"
        (db/all-products (d/as-of (d/db conn) inst-1)))
      (group
        "using as-of at instant 2"
        (db/all-products (d/as-of (d/db conn) inst-2))))))

(defn run-schema-examples
  [conn]
  (let [{phone-id :db/id} (db/first-product-with-path (d/db conn) "/phone")]
    (group
      "schema examples"
      (group
        "evolve schema"
        (db/first-product-with-path (d/db conn) "/phone")
        @(d/transact conn db/schema-with-tag))
      (group
        "add tags to phone"
        @(d/transact conn
                     [[:db/add phone-id :product/tag "mobile"]
                      [:db/add phone-id :product/tag "android"]])
        (db/first-product-with-path (d/db conn) "/phone")))))

(defn run-category-examples
  [conn]
  (let [category-sports (model/new-category "Sports")
        category-electronics (model/new-category electronics-id "Electronics")]
    (group
      "category examples"
      (group
        "register category schemas"
        db/schema-with-category
        @(d/transact conn db/schema-with-category))
      (group
        "create categories"
        @(d/transact conn [category-sports category-electronics]))
      (group
        "list categories"
        (db/all-categories (d/db conn)))
      (group
        "Associate category with entity ids"
        @(d/transact conn [[:db/add
                            (db/uuid->eid (d/db conn) :product/id computer-id)
                            :product/category
                            (db/uuid->eid (d/db conn) :category/id electronics-id)]])
        (db/all-products-* (d/db conn)))
      (group
          "Associate category with uuids"
          @(d/transact conn [[:db/add
                              [:product/id computer-id]
                              :product/category
                              [:category/id electronics-id]]]))
      (group
        "Associate using function"
        )
      )))

#_(def conn (db/connect-to-db))

#_(-> (get-fresh-database-conn)
      (seed-database)
      (run-db-examples))

#_(-> (get-fresh-database-conn)
      (seed-database)
      (run-category-examples))

#_(-> (get-fresh-database-conn)
      (seed-database)
      (run-schema-examples))

#_(-> (get-fresh-database-conn)
      (seed-database)
      (run-time-examples))

#_(-> (get-fresh-database-conn)
      (seed-database)
      (run-query-examples))

;; class 3.2
;; https://github.com/alura-cursos/datomic-identidades-e-queries/tree/master/aula3.2
;; alexandre.aquiles@caelum.com.br 