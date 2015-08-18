package geotrellis.spark.etl.cassandra

import com.datastax.spark.connector.cql.CassandraConnector
import geotrellis.spark.etl.OutputPlugin

import geotrellis.spark._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.utils.SparkUtils
import org.apache.spark.{SparkContext, SparkConf}
import scala.reflect._

trait CassandraOutput extends OutputPlugin {
  val name = "cassandra"
  val requiredKeys = Array("host", "keyspace", "table")

  // Attention, spark.cassandra.connection.host needs to be set
  // at the creation of the spark context, eg with spark-submit --conf
  def getSession(props: Map[String, String]) : CassandraSession = {
    val sparkConf = new SparkConf()
    sparkConf.set("spark.cassandra.connection.host", props("host"))
    val connector = CassandraConnector(sparkConf)
    new CassandraSession(connector, props("keyspace"))
  }

  def attributes(props: Map[String, String]) = {
    implicit val session = getSession(props)
    CassandraAttributeStore("attributes")
  }
}
