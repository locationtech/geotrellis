package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.accumulo.AccumuloAttributeStore

trait AccumuloOutput[K, V, M] extends OutputPlugin[K, V, M] {
  val name = "accumulo"
  val requiredKeys = Array("instance", "zookeeper", "user", "password", "table")

  def attributes(props: Map[String, String]) = AccumuloAttributeStore(getInstance(props).connector, props("table"))
}
