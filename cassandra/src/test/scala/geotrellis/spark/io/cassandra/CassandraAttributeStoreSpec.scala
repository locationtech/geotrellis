package geotrellis.spark.io.cassandra

import geotrellis.spark.io.AttributeStoreSpec

class CassandraAttributeStoreSpec extends AttributeStoreSpec {
  lazy val instance       = BaseCassandraInstance(Seq("127.0.0.1"), "geotrellis")
  lazy val attributeStore = try {
    new CassandraAttributeStore(instance, "attributes")
  } catch {
    case e: Exception =>
      println("A script for setting up the Cassandra environment necessary to run these tests can be found at scripts/cassandraTestDB.sh - requires a working docker setup")
      throw e
  }
}

