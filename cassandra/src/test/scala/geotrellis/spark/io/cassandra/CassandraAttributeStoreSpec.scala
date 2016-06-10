package geotrellis.spark.io.cassandra

import geotrellis.spark.CassandraTestEnvironment
import geotrellis.spark.io.AttributeStoreSpec

class CassandraAttributeStoreSpec extends AttributeStoreSpec with CassandraTestEnvironment {
  lazy val instance       = BaseCassandraInstance(Seq("127.0.0.1"))
  lazy val attributeStore = new CassandraAttributeStore(instance, "geotrellis", "attributes")
}
