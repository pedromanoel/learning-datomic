(ns learning-datomic.ecommerce.model)

(defn new-product [name path price]
  {:product/name  name
   :product/path  path
   :product/price price})