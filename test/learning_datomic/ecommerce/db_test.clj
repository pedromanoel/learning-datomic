(ns learning-datomic.ecommerce.db-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d])
  (:import (datomic.db Datum)))

(def db-uri "datomic:mem://hello")
(defn conn [] (d/connect db-uri))
(defn db [] (d/db (conn)))

(defn with-clojure-in-memory [f]
  (d/create-database db-uri)
  (f)
  (d/delete-database db-uri))

(use-fixtures :each with-clojure-in-memory)

;; TODO criar testes unitários para os conceitos aprendidos aqui
;; adicionar entidades
;; nil
;; atributos não declarados
;; result é future
;; os campos do result
;; os atoms retornados
;; ver se tem funcoes para ler eavt do atomo
;; schema gera atomos
;; tipos sao atomos

(def built-in-attributes-q
  '{:find  [(pull ?attribute [:db/id :db/ident :db/doc])]
    :where [[?attribute :db/ident]]})

(def attribute-id-q
  '{:find  [?part-db]
    :in    [$ ?attribute-name]
    :where [[?part-db :db/ident ?attribute-name]]})

(defn eid-of [db attribute-ident]
  (ffirst (d/q attribute-id-q db attribute-ident)))

(def test-schema {:db/ident       :test/string
                  :db/valueType   :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc         "A test string"})

(defn def-schema-single [ident type doc]
  {:db/ident       ident
   :db/valueType   type
   :db/cardinality :db.cardinality/one
   :db/doc         doc})

(def credit-schema
  [(def-schema-single :customer/id :db.type/uuid "Customer's global ID")
   (def-schema-single :account/customer :db.type/ref "The customer for this account")
   (def-schema-single :account/limit :db.type/bigdec "How much is the limit for this account")
   (def-schema-single :card/account :db.type/ref "The account where this card transacts")
   (def-schema-single :card/status :db.type/symbol "If the card is active/inactive")])

(deftest datomic-superpowers-test
  (testing "changing facts over time"
    ))

(deftest built-in-attributes-test
  (let [attributes (->> (d/q built-in-attributes-q (db))
                        (flatten)
                        (map #(hash-map (:db/ident %) [(:db/id %) (:db/doc %)]))
                        (group-by #(keyword (namespace (first (keys %))))))]
    (is (= {:db             [#:db{:add [1 "Primitive assertion. All transactions eventually reduce to a collection of primitive assertions and retractions of facts, e.g. [:db/add fred :age 42]."]}
                             #:db{:retract [2 "Primitive retraction. All transactions eventually reduce to a collection of assertions and retractions of facts, e.g. [:db/retract fred :age 42]."]}
                             #:db{:system-tx [7 nil]}
                             #:db{:ident [10 "Attribute used to uniquely name an entity."]}
                             #:db{:excise [15 nil]}
                             #:db{:valueType [40 "Property of an attribute that specifies the attribute's value type. Built-in value types include, :db.type/keyword, :db.type/string, :db.type/ref, :db.type/instant, :db.type/long, :db.type/bigdec, :db.type/boolean, :db.type/float, :db.type/uuid, :db.type/double, :db.type/bigint,  :db.type/uri."]}
                             #:db{:cardinality [41 "Property of an attribute. Two possible values: :db.cardinality/one for single-valued attributes, and :db.cardinality/many for many-valued attributes. Defaults to :db.cardinality/one."]}
                             #:db{:unique [42 "Property of an attribute. If value is :db.unique/value, then attribute value is unique to each entity. Attempts to insert a duplicate value for a temporary entity id will fail. If value is :db.unique/identity, then attribute value is unique, and upsert is enabled. Attempting to insert a duplicate value for a temporary entity id will cause all attributes associated with that temporary id to be merged with the entity already in the database. Defaults to nil."]}
                             #:db{:isComponent [43 "Property of attribute whose vtype is :db.type/ref. If true, then the attribute is a component of the entity referencing it. When you query for an entire entity, components are fetched automatically. Defaults to nil."]}
                             #:db{:index [44 "Property of an attribute. If true, create an AVET index for the attribute. Defaults to false."]}
                             #:db{:noHistory [45 "Property of an attribute. If true, past values of the attribute are not retained after indexing. Defaults to false."]}
                             #:db{:lang [46 "Attribute of a data function. Value is a keyword naming the implementation language of the function. Legal values are :db.lang/java and :db.lang/clojure"]}
                             #:db{:code [47 "String-valued attribute of a data function that contains the function's source code."]}
                             #:db{:txInstant [50 "Attribute whose value is a :db.type/instant. A :db/txInstant is recorded automatically with every transaction."]}
                             #:db{:fulltext [51 "Property of an attribute. If true, create a fulltext search index for the attribute. Defaults to false."]}
                             #:db{:fn [52 "A function-valued attribute for direct use by transactions and queries."]}
                             #:db{:retractEntity [54 "Retract all facts about an entity, including references from other entities and component attributes recursively."]}
                             #:db{:cas [55 "Compare and swap the value of an entity's attribute."]}
                             #:db{:doc [62 "Documentation string for an entity."]}
                             #:db{:tupleType [65 nil]}
                             #:db{:tupleTypes [66 nil]}
                             #:db{:tupleAttrs [67 nil]}
                             #:db{:ensure [68 nil]}]
            :db.alter       [#:db.alter{:attribute [19 "System attribute with type :db.type/ref. Asserting this attribute on :db.part/db with value v will alter the definition of existing attribute v."]}]
            :db.attr        [#:db.attr{:preds [71 nil]}]
            :db.bootstrap   [#:db.bootstrap{:part [53 nil]}]
            :db.cardinality [#:db.cardinality{:one [35 "One of two legal values for the :db/cardinality attribute. Specify :db.cardinality/one for single-valued attributes, and :db.cardinality/many for many-valued attributes."]}
                             #:db.cardinality{:many [36 "One of two legal values for the :db/cardinality attribute. Specify :db.cardinality/one for single-valued attributes, and :db.cardinality/many for many-valued attributes."]}]
            :db.entity      [#:db.entity{:attrs [69 nil]}
                             #:db.entity{:preds [70 nil]}]
            :db.excise      [#:db.excise{:attrs [16 nil]}
                             #:db.excise{:beforeT [17 nil]}
                             #:db.excise{:before [18 nil]}]
            :db.install     [#:db.install{:partition [11 "System attribute with type :db.type/ref. Asserting this attribute on :db.part/db with value v will install v as a partition."]}
                             #:db.install{:valueType [12 "System attribute with type :db.type/ref. Asserting this attribute on :db.part/db with value v will install v as a value type."]}
                             #:db.install{:attribute [13 "System attribute with type :db.type/ref. Asserting this attribute on :db.part/db with value v will install v as an attribute."]}
                             #:db.install{:function [14 "System attribute with type :db.type/ref. Asserting this attribute on :db.part/db with value v will install v as a data function."]}]
            :db.lang        [#:db.lang{:clojure [48 "Value of :db/lang attribute, specifying that a data function is implemented in Clojure."]}
                             #:db.lang{:java [49 "Value of :db/lang attribute, specifying that a data function is implemented in Java."]}]
            :db.part        [#:db.part{:db [0 "Name of the system partition. The system partition includes the core of datomic, as well as user schemas: type definitions, attribute definitions, partition definitions, and data function definitions."]}
                             #:db.part{:tx [3 "Partition used to store data about transactions. Transaction data always includes a :db/txInstant which is the transaction's timestamp, and can be extended to store other information at transaction granularity."]}
                             #:db.part{:user [4 "Name of the user partition. The user partition is analogous to the default namespace in a programming language, and should be used as a temporary home for data during interactive development."]}]
            :db.sys         [#:db.sys{:partiallyIndexed [8 "System-assigned attribute set to true for transactions not fully incorporated into the index"]}
                             #:db.sys{:reId [9 "System-assigned attribute for an id e in the log that has been changed to id v in the index"]}]
            :db.type        [#:db.type{:ref [20 "Value type for references. All references from one entity to another are through attributes with this value type."]}
                             #:db.type{:keyword [21 "Value type for keywords. Keywords are used as names, and are interned for efficiency. Keywords map to the native interned-name type in languages that support them."]}
                             #:db.type{:long [22 "Fixed integer value type. Same semantics as a Java long: 64 bits wide, two's complement binary representation."]}
                             #:db.type{:string [23 "Value type for strings."]}
                             #:db.type{:boolean [24 "Boolean value type."]}
                             #:db.type{:instant [25 "Value type for instants in time. Stored internally as a number of milliseconds since midnight, January 1, 1970 UTC. Representation type will vary depending on the language you are using."]}
                             #:db.type{:fn [26 "Value type for database functions. See Javadoc for Peer.function."]}
                             #:db.type{:bytes [27 "Value type for small binaries. Maps to byte array on the JVM."]}
                             #:db.type{:uuid [56 "Value type for UUIDs. Maps to java.util.UUID on the JVM."]}
                             #:db.type{:double [57 "Floating point value type. Same semantics as a Java double: double-precision 64-bit IEEE 754 floating point."]}
                             #:db.type{:float [58 "Floating point value type. Same semantics as a Java float: single-precision 32-bit IEEE 754 floating point."]}
                             #:db.type{:uri [59 "Value type for URIs. Maps to java.net.URI on the JVM."]}
                             #:db.type{:bigint [60 "Value type for arbitrary precision integers. Maps to java.math.BigInteger on the JVM."]}
                             #:db.type{:bigdec [61 "Value type for arbitrary precision floating point numbers. Maps to java.math.BigDecimal on the JVM."]}
                             #:db.type{:tuple [63 nil]}
                             #:db.type{:symbol [64 nil]}]
            :db.unique      [#:db.unique{:value [37 "Specifies that an attribute's value is unique. Attempts to create a new entity with a colliding value for a :db.unique/value will fail."]}
                             #:db.unique{:identity [38 "Specifies that an attribute's value is unique. Attempts to create a new entity with a colliding value for a :db.unique/value will become upserts."]}]
            :fressian       [#:fressian{:tag [39 "Keyword-valued attribute of a value type that specifies the underlying fressian type used for serialization."]}]} attributes))))

(deftest tempid-test
  (testing "generate monotonically decreasing tempids"
    (let [{tempid :idx} (d/tempid :db.part/tx)
          {later-tempid :idx} (d/tempid :db.part/tx)]
      (is (> tempid later-tempid)))))

(deftest transact-return-test
  (let [db (db)
        tx-eid (eid-of db :db/txInstant)
        part-eid (eid-of db :db.part/db)
        string-type-eid (eid-of db :db.type/string)
        card-one-eid (eid-of db :db.cardinality/one)
        tx-return @(d/transact (conn) [test-schema])
        {:keys [tx-data db-before db-after tempids]} tx-return
        [first-datum
         entity-attr-datum
         entity-type-datum
         entity-card-datum
         entity-doc-datum
         last-datum] tx-data]
    (testing "Transaction returns a map"
      (is (map? tx-return))
      (is (= [:db-before :db-after :tx-data :tempids] (keys tx-return))))
    (testing "tx-data contains atoms"
      (is (every? #(= Datum (type %)) tx-data)))
    (testing "first datum contains the transaction"
      (is (= tx-eid (:a first-datum)) "the datum's attribute has type :db/txInstant")
      (is (inst? (:v first-datum)) "the datum's value is an instant")
      (is (= (:e first-datum) (:tx first-datum)) "the datum's tx references its own eid"))
    (testing "last datum contains a :db.part/db entity"
      (is (= part-eid (:e last-datum))))
    (testing "all datom have the same transaction id"
      (is (apply = (map :tx tx-data)))
      (is (= 6 (count tx-data))))
    (testing "second datum is the transacted schema"
      (is (= (:e entity-attr-datum) (:e entity-type-datum) (:e entity-card-datum) (:e entity-doc-datum)) "all datom have the same entity id")
      (is (= :test/string (:v entity-attr-datum)))
      (is (= string-type-eid (:v entity-type-datum)))
      (is (= card-one-eid (:v entity-card-datum)))
      (is (= "A test string" (:v entity-doc-datum))))
    ))
