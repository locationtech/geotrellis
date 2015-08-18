package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.InputPlugin

trait AccumuloInput extends InputPlugin {

  def name = "accumulo"
  def format = "catalog"

  val requiredKeys = Array("instance", "zookeeper", "user", "password", "table", "layer") // optional: bbox

}