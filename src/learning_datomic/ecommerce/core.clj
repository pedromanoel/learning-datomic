(ns learning-datomic.ecommerce.core
  (:require [clojure.pprint :refer [pprint]])
  (:require [datomic.api :as d]
            (learning-datomic.ecommerce
              [db :as db]
              [model :as model])))

(defn print-header [title]
  (println (str "--- " title " ---")))

(defn print-session [session-name & values]
  (print-header session-name)
  (doseq [value values]
    (pprint value))
  (println))

(defn setup-database []
  (db/erase-db)
  (let [conn (db/connect-to-db)
        computer (model/new-product "Computer" "/computer" 2500.10M)
        phone (model/new-product "Phone" "/phone" 888.88M)
        calculator {:product/name "Calculator"}
        radio {:product/path nil}                           ;; can't transact nil values
        bad-path {:product/paths "/path"}                   ;; can't transact unknown attributes
        tv (model/new-product "TV" "/tv" 33000.00M)]
    (db/create-schema conn)
    (pprint (d/transact conn [computer phone calculator #_radio #_bad-path])) ;; atomic operation
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
      (pprint @(d/transact conn [[:db/retract entity-id :product/price]])))

    (print-session
      "Query with params"
      (db/all-product-entity-ids (d/db conn)))

    (print-session
      "Query with params"
      (db/all-products-with-path (d/db conn) "/computer")
      (db/all-products-with-path (d/db conn) "/tv")
      (db/all-products-with-path (d/db conn) "/dont-exist"))

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
      "Query example with explicit :in"
      (d/q '[:find ?e
             :in $
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (print-session
      "Query example with explicit :in"
      (d/q '[:find ?e
             :in $
             :where [?e :product/path "/computer"]]
           (d/db conn)))

    (print-session
      "Cartesian names and prices"
      (db/product-names-and-prices-cartesian (d/db conn)))

    (print-session
      "Same entities names and prices"
      (db/product-names-and-prices (d/db conn)))

    (print-session
      "Select everything (does not work)"
      (d/q '[:find ?e ?a ?v ?t
             :where [?e ?a ?v ?t]]
           (d/db conn)))))

#_(setup-database)
