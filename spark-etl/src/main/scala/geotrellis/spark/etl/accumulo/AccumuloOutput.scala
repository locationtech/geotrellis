package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.accumulo.AccumuloAttributeStore

trait AccumuloOutput extends OutputPlugin {
  val name = "accumulo"
  val requiredKeys = Array("instance", "zookeeper", "user", "password", "table")

  def attributes(props: Map[String, String]) = AccumuloAttributeStore(getInstance(props).connector, props("table"))
}
