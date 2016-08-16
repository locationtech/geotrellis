package geotrellis.spark.etl.hbase

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.hbase.HBaseAttributeStore

trait HBaseOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "hbase"
  val requiredKeys = Array("master", "zookeepers", "table")
  
  def attributes(props: Map[String, String]) = HBaseAttributeStore(getInstance(props), props("table"))
}
