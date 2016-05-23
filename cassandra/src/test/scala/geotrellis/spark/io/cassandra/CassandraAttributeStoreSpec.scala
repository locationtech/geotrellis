package geotrellis.spark.io.cassandra

import geotrellis.spark.io.AttributeStoreSpec

class CassandraAttributeStoreSpec extends AttributeStoreSpec {
  lazy val instance       = BaseCassandraInstance(Seq("127.0.0.1"), "geotrellis")
  lazy val attributeStore = new CassandraAttributeStore(instance, "attributes")
}

