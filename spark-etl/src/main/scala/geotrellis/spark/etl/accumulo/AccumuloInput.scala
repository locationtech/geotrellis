package geotrellis.spark.etl.accumulo

import geotrellis.spark.LayerId
import geotrellis.spark.etl.{CatalogInputPlugin, InputPlugin}
import geotrellis.vector.Extent

trait AccumuloInput[K] extends CatalogInputPlugin[K] {
  def name = "accumulo"
  override def requiredKeys = super.requiredKeys ++ Array("instance", "zookeeper", "user", "password", "table") // optional: bbox
}