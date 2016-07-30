package geotrellis.spark.etl.hbase

import geotrellis.spark.etl.{EtlJob, OutputPlugin}
import geotrellis.spark.io.hbase.HBaseAttributeStore

import com.typesafe.scalalogging.slf4j.LazyLogging

trait HBaseOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "hbase"
  
  def attributes(job: EtlJob) = HBaseAttributeStore(getInstance(job.conf.outputProfile), job.outputProps("table"))
}
