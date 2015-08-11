package geotrellis.spark.io.cassandra

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.embedded.{ EmbeddedCassandra => DataStaxEmbeddedCassandra }

/** Used for IT tests. */
trait SharedEmbeddedCassandra extends DataStaxEmbeddedCassandra {

  def clearCache(): Unit = CassandraConnector.evictCache()

  def getHost() = DataStaxEmbeddedCassandra.getHost(0)
}
