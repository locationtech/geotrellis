package geotrellis.spark.etl.hbase

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.hbase.HBaseAttributeStore
import geotrellis.spark.etl.config.EtlConf

trait HBaseOutput[K, V, M] extends OutputPlugin[K, V, M] {
  val name = "hbase"
  
  def attributes(conf: EtlConf) = HBaseAttributeStore(getInstance(conf.outputProfile))
}
