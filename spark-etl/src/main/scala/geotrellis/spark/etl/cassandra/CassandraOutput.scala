package geotrellis.spark.etl.cassandra

import geotrellis.spark.etl.OutputPlugin

import geotrellis.spark._
import geotrellis.spark.io.cassandra._

trait CassandraOutput extends OutputPlugin {
  val name = "cassandra"
  val requiredKeys = Array("host", "keyspace", "table")

  // Attention, spark.cassandra.connection.host needs to be set
  // at the creation of the spark context, eg with spark-submit --conf
}
