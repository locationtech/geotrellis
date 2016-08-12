package geotrellis.spark.etl.hbase

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.hbase.HBaseAttributeStore
import geotrellis.spark.etl.config.EtlConf

import com.typesafe.scalalogging.slf4j.LazyLogging

trait HBaseOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "hbase"
  
  def attributes(conf: EtlConf) = HBaseAttributeStore(getInstance(conf.outputProfile), getPath(conf.output.backend).table)
}
