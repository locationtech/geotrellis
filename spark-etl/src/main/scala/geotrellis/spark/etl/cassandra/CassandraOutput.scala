package geotrellis.spark.etl.cassandra

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.io.cassandra.CassandraAttributeStore

trait CassandraOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "cassandra"
  val requiredKeys = Array("host", "user", "password", "keyspace", "table")
  
  def attributes(props: Map[String, String], credentials: Option[Backend]) = CassandraAttributeStore(getInstance(credentials))
}
