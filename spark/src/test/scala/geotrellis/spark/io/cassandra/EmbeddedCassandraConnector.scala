package geotrellis.spark.io.cassandra

import com.datastax.driver.core.Session
import com.datastax.spark.connector.cql._
import org.apache.spark.SparkContext
import org.apache.spark.Logging

import java.net.InetAddress

object EmbeddedCassandra extends Logging {

  val GtCassandraTestKeyspace = "test"

  val deleteKeyspaceCql = s"DROP KEYSPACE IF EXISTS ${GtCassandraTestKeyspace}"
  val createKeyspaceCql = s"CREATE KEYSPACE IF NOT EXISTS ${GtCassandraTestKeyspace} WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }"
  
  // Recreate the keyspace with each session, and prevent premature closure of session.
  def withSession(host: String, rpcPort: Int, nativePort: Int, keySpace: String)(f: CassandraSession => Unit)(implicit sc: SparkContext): Unit = {
    val sparkConf = sc.getConf
    sparkConf.set("spark.cassandra.connection.host", host)
      .set("spark.cassandra.connection.rpc.port", rpcPort.toString)
      .set("spark.cassandra.connection.native.port", nativePort.toString)

    println(s"set config in EmbeddedCassandra ${GtCassandraTestKeyspace} host ${host} rpc ${rpcPort.toString} native ${nativePort.toString}")

    val testValue = sparkConf.get("spark.cassandra.connection.host")
    println(s"get sparkConf in EmbeddedCassandra ${testValue} host ")

    val hadoopConf = sc.hadoopConfiguration
    val connector = CassandraConnector(sparkConf)
    val session = new CassandraSession(connector, keySpace)
    session.execute(deleteKeyspaceCql)
    session.execute(createKeyspaceCql)

    try {
      f(session)
    } finally {
      logInfo("finally in EmbeddedCassandraSession")
    }
  }
}
