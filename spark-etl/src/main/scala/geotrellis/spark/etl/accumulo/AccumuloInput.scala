package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.CatalogInputPlugin

trait AccumuloInput[K, V, M] extends CatalogInputPlugin[K, V, M] {
  def name = "accumulo"
  override def requiredKeys = super.requiredKeys ++ Array("instance", "zookeeper", "user", "password", "table") // optional: bbox
}