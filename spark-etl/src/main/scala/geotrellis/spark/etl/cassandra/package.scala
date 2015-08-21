package geotrellis.spark.etl

import com.datastax.spark.connector.cql.CassandraConnector
import geotrellis.spark.io.cassandra.CassandraSession
import org.apache.spark.SparkConf

/**
 * Created by akmoch on 21/08/15.
 */
package object cassandra {

  // Attention, spark.cassandra.connection.host needs to be set
  // at the creation of the spark context, eg with spark-submit --conf
  def getSession(props: Map[String, String]) : CassandraSession = {
    val sparkConf = new SparkConf()
    sparkConf.set("spark.cassandra.connection.host", props("host"))
    val connector = CassandraConnector(sparkConf)
    new CassandraSession(connector, props("keyspace"))
  }
}
