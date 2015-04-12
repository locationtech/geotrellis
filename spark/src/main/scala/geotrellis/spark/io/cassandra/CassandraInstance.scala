package geotrellis.spark.io.cassandra

import geotrellis.spark.io._

import com.datastax.spark.connector.cql._
import org.apache.spark.SparkContext

import com.typesafe.config.{ConfigFactory,Config}

case class CassandraInstance(
  connector: CassandraConnector, 
  keyspace: String
) {
  
  val session = connector.openSession()
  def close() = session.close
}
