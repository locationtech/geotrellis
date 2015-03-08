package geotrellis.spark.io.cassandra

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.embedded.EmbeddedCassandra

/** Used for IT tests. */
trait SharedEmbeddedCassandra extends EmbeddedCassandra {

  def clearCache(): Unit = CassandraConnector.evictCache()

}
