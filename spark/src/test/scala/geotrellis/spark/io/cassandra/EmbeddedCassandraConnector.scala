package geotrellis.spark.io.cassandra

import com.datastax.spark.connector.cql._
import org.apache.spark.SparkContext

import java.net.InetAddress

object EmbeddedCassandra {

  val GtCassandraTestKeyspace = "geotrellistest"

  val deleteKeyspaceCql = s"DROP KEYSPACE IF EXISTS ${GtCassandraTestKeyspace}"
  val createKeyspaceCql = s"CREATE KEYSPACE IF NOT EXISTS ${GtCassandraTestKeyspace} WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }"
  
  // Recreate the keyspace with each session, and prevent premature closure of session.
  def withSession(host: String, nativePort: Int, keySpace: String)(f: CassandraSession => Unit)(implicit sc: SparkContext): Unit = {
    var sparkConf = sc.getConf
    sparkConf.set("spark.cassandra.connection.host", host)
    sparkConf.set("spark.cassandra.connection.native.port", nativePort.toString)
    val hadoopConf = sc.hadoopConfiguration
    val connector = CassandraConnector(sparkConf)
    val session = new CassandraSession(connector, keySpace)
    session.execute(deleteKeyspaceCql)
    session.execute(createKeyspaceCql)
    f(session)
  }
}
