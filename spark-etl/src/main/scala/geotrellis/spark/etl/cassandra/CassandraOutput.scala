package geotrellis.spark.etl.cassandra

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io.cassandra.CassandraAttributeStore

import com.typesafe.scalalogging.slf4j.LazyLogging

trait CassandraOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "cassandra"
  
  def attributes(conf: EtlConf) = CassandraAttributeStore(getInstance(conf.outputProfile))
}
