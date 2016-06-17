package geotrellis.spark.etl.cassandra

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark.etl.{EtlJob, OutputPlugin}
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io.cassandra.CassandraAttributeStore

trait CassandraOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "cassandra"
  
  def attributes(job: EtlJob) = CassandraAttributeStore(getInstance(job.outputCredentials))
}
