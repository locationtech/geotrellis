package geotrellis.spark.io.cassandra

import geotrellis.spark.io._

import com.datastax.spark.connector.cql._
import com.datastax.driver.core.Session
import org.apache.spark.SparkContext

import com.typesafe.config.{ConfigFactory,Config}

object Cassandra {
  def withSession(host: String, keySpace: String)(f: CassandraSession => Unit)(implicit sc: SparkContext): Unit = {
    val sparkConf = sc.getConf
    val hadoopConf = sc.hadoopConfiguration
    sparkConf.set("spark.cassandra.connection.host", host)
    val connector = CassandraConnector(sparkConf)
    val cassandra = new CassandraSession(connector, keySpace)
    try {
      f(cassandra)
    } finally {
      cassandra.close()
    }
  }
}

object CassandraSession {
  implicit def cassandraSessionToDataStaxSession(session: CassandraSession): Session =
    session.session
}

class CassandraSession( connector: CassandraConnector, val keySpace: String ) {
  private val session: Session = connector.openSession()
  def close() = session.close
}
