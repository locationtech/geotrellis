package geotrellis.spark.io.cassandra

import geotrellis.spark.io.AttributeStoreSpec

class CassandraAttributeStoreSpec extends AttributeStoreSpec {
  lazy val instance       = BaseCassandraInstance(Seq("localhost"), "geotrellis")
  lazy val attributeStore = new CassandraAttributeStore(instance, "attributes")
}

