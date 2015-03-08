package geotrellis.spark.io.cassandra

import com.datastax.driver.core.Session
import com.datastax.spark.connector.cql._

import java.net.InetAddress
import org.apache.spark.SparkConf

class EmbeddedCassandraConnector(conf: CassandraConnectorConf) extends CassandraConnector(conf) {

  val createKeyspaceCql = "CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }"

  /* Create test keyspace before interacting with each session */
  override def withSessionDo[T](code: Session => T): T = {
    closeResourceAfterUse(openSession()) { session =>
      val proxy: Session = SessionProxy.wrap(session)
      proxy.execute(createKeyspaceCql)
      code(proxy)
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
