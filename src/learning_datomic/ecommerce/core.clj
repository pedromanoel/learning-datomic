(ns learning-datomic.ecommerce.core
  (:require [clojure.pprint :refer [pprint]])
  (:require [datomic.api :as d]
            (learning-datomic.ecommerce
              [db :as db]
              [model :as model]))
  (:import (java.util Date)))

(defn print-header [title]
  (println (str "--- " title " ---")))

(defn print-session [session-name & values]
  (print-header session-name)
  (doseq [value values]
    (clojure.edn/read-string (pprint value)))
  (println))

(defn setup-database []
  (db/erase-db)
  (let [conn (db/connect-to-db)
        computer (model/new-product "Computer" "/computer" 2500.10M)
        phone (model/new-product "Phone" "/phone" 888.88M)
        calculator {:product/name "Calculator"}
        tv (model/new-product "TV" "/tv" 33000.00M)
        product-nil-val {:product/path nil}
        product-bad-path {:product/paths "/path"}]
    (db/create-schema conn)
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
      (pprint @(d/transact conn [[:db/retract entity-id :product/price]])))))

(defn run-query-examples
  []
  (let [conn (db/connect-to-db)
        db (d/db conn)]

    (print-session
      "Query example"
      (d/q '[:find ?e
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (print-session
      "Query all paths"
      (db/all-paths (d/db conn)))

    (print-session
      "Query all prices"
      (db/all-prices (d/db conn)))

    (print-session
      "Query example with implicit :in"
      (d/q '[:find ?e
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (print-session
      "Query example with explicit :in"
      (d/q '[:find ?e
             :in $
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (print-session
      "Query example with parameters"
      (d/q '[:find ?e ?path
             :in $ ?path
             :where [?e :product/path ?path]]
           (d/db conn)
           "/computer"))

    (print-session
      "Query example with keys"
      (d/q '[:find ?e ?path
             :keys product/id product/path
             :where [?e :product/path ?path]]
           (d/db conn)))

    (print-session
      "Select everything (does not work because there are no bindings)"
      #_(d/q '[:find ?e ?a ?v ?t
               :where [?e ?a ?v ?t]]
             (d/db conn)))

    (print-session
      "Select schemas"
      (d/q '[:find (pull ?e [*])
             :where [?e :db/ident]]
           (d/db conn)))))

(defn run-db-examples
  []
  (let [db (d/db (db/connect-to-db))]
    (print-session "Query with params" (db/all-product-entity-ids db))

    (print-session
      "Query with params"
      (db/all-products-with-path db "/computer")
      (db/all-products-with-path db "/tv")
      (db/all-products-with-path db "/dont-exist"))
    (print-session "Cartesian names and prices" (db/product-names-and-prices-cartesian db))
    (print-session "Same entities names and prices" (db/product-names-and-prices db))
    (print-session "All products with all attrs" (db/all-products-with-all-attrs db))
    (print-session "All products" (db/all-products db))
    (print-session "All products with all attrs" (db/all-products-* db))
    (print-session "With predicates" (db/all-products-with-prices-greater-than db 1000M))))

(defn run-time-examples
  []
  (let [conn (db/connect-to-db)
        inst-1 (Date.)
        db-1 (d/db conn)
        chair (model/new-product "Chair" "/chair" 199.99M)
        mouse (model/new-product "Mouse" "/mouse" 25.00M)
        {db-2 :db-after} @(d/transact conn [chair mouse])
        inst-2 (Date.)]
    (print-session
      "Add first two products"
      (db/all-products db-1))
    (print-session
      "After adding two more products"
      (db/all-products db-2))
    (print-session
      "Using as-of before"
      (db/all-products (d/as-of (d/db conn) inst-1)))
    (print-session
      "Using as-of after"
      (db/all-products (d/as-of (d/db conn) inst-2)))
    ))

(defn run-schema-examples
  []
  (let [conn (db/connect-to-db)]
    (let [{phone-id :db/id} (db/first-product-with-path (d/db conn) "/phone")]
      (print-session
        "evolve schema"
        (db/first-product-with-path (d/db conn) "/phone")
        @(d/transact conn db/new-schema))
      (print-session
        "add tags to phone"
        @(d/transact conn
                     [[:db/add phone-id :product/tag "mobile"]
                      [:db/add phone-id :product/tag "android"]])
        (db/first-product-with-path (d/db conn) "/phone")))))

#_(do
    (setup-database)
    (run-db-examples))

#_(do
    (setup-database)
    (run-schema-examples))

#_(do
    (setup-database)
    (run-time-examples))

#_(do
    (setup-database)
    (run-query-examples))
