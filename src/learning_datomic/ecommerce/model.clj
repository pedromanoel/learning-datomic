(ns learning-datomic.ecommerce.model
  (:import (java.util UUID)))

(defn uuid []
  (UUID/randomUUID))

(defn new-product
  ([name path price]
   (new-product (uuid) name path price))
  ([uuid name path price]
   {:product/id    uuid
    :product/name  name
    :product/path  path
    :product/price price}))

(defn new-category
  ([name] (new-category (uuid) name))
  ([uuid name] {:category/id uuid :category/name name}))