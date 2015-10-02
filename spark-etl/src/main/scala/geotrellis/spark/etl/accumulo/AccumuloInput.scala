package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.CatalogInputPlugin

trait AccumuloInput[K] extends CatalogInputPlugin[K] {
  def name = "accumulo"
  override def requiredKeys = super.requiredKeys ++ Array("instance", "zookeeper", "user", "password", "table") // optional: bbox
}