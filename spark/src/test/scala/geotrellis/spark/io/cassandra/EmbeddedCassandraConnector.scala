package geotrellis.spark.io.cassandra

import com.datastax.driver.core.Session
import com.datastax.spark.connector.cql._

import java.net.InetAddress
import org.apache.spark.SparkConf

class EmbeddedCassandraConnector(conf: CassandraConnectorConf) extends CassandraConnector(conf) {

  val deleteKeyspaceCql = "DROP KEYSPACE IF EXISTS test"
  val createKeyspaceCql = "CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }"

  // Recreate keyspace on creation to start with clean state
  {
    withSessionDo { session =>
      session.execute(deleteKeyspaceCql)
      session.execute(createKeyspaceCql)
    }
  }
}

object EmbeddedCassandraConnector {

  def apply(conf: SparkConf): EmbeddedCassandraConnector = {
    new EmbeddedCassandraConnector(CassandraConnectorConf(conf))
  }

  def apply(hosts: Set[InetAddress]) = {
    val config = CassandraConnectorConf(hosts)
    new EmbeddedCassandraConnector(config)
  }
}
