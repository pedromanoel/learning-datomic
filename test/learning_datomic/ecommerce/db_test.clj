(ns learning-datomic.ecommerce.db-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]))

(def db-uri "datomic:mem://hello")
(defn conn [] (d/connect "datomic:mem://hello"))

(defn with-clojure-in-memory [f]
  (d/create-database db-uri)
  (f)
  (d/delete-database db-uri))

(use-fixtures :each with-clojure-in-memory)


;; TODO criar testes unitários para os conceitos aprendidos aqui
;; criar schema
;; adicionar entidades
;; nil
;; atributos não declarados
;; result é future
;; os campos do result
;; os atoms retornados
;; ver se tem funcoes para ler eavt do atomo
;; schema gera atomos
;; tipos sao atomos

(deftest transact-schema-test
  (let [name-schema {:db/ident       :test/name
                     :db/valueType   :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc         "A name that is a string"}
        {:keys [tx-data]} @(d/transact (conn) [name-schema])]
    (testing "first datom is a transaction"
      (let [first-datum (first tx-data)
            {:keys [e a v]} first-datum]
        (is (number? e))
        (is (number? a))
        (is (inst? v))))
    ))