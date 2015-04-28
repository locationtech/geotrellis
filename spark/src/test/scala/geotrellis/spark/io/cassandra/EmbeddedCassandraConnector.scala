package geotrellis.spark.io.cassandra

import com.datastax.spark.connector.cql._
import org.apache.spark.SparkContext

import java.net.InetAddress

object EmbeddedCassandra {

  val deleteKeyspaceCql = "DROP KEYSPACE IF EXISTS test"
  val createKeyspaceCql = "CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }"
  
  // Recreate the keyspace with each session, and prevent premature closure of session.
  def withSession(host: String, keySpace: String)(f: CassandraSession => Unit)(implicit sc: SparkContext): Unit = {
    val sparkConf = sc.getConf
    val hadoopConf = sc.hadoopConfiguration
    sparkConf.set("spark.cassandra.connection.host", host)
    val connector = CassandraConnector(sparkConf)
    val session = new CassandraSession(connector, keySpace)
    session.execute(deleteKeyspaceCql)
    session.execute(createKeyspaceCql)
    f(session)
  }
}
