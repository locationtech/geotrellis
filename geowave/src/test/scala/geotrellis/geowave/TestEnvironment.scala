package geotrellis.geowave

import org.scalatest.funspec.AnyFunSpec

import scala.util.Properties
import org.locationtech.geowave.core.store.api.DataStore
import org.locationtech.geowave.core.store.api.DataStoreFactory
import org.locationtech.geowave.datastore.cassandra.config.{CassandraOptions, CassandraRequiredOptions}

class TestEnvironment extends AnyFunSpec {
  val kafka: String     = Properties.envOrElse("KAFKA_HOST", "localhost:9092")
  val cassandra: String = Properties.envOrElse("CASSANDRA_HOST", "localhost")

  def getDataStore(name: String): DataStore = {
    DataStoreFactory.createDataStore(
      new CassandraRequiredOptions(cassandra, name,
      new CassandraOptions()))
  }
}
