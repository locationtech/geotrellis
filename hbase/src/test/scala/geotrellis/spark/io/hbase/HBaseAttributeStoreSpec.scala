package geotrellis.spark.io.hbase

import geotrellis.spark.TestEnvironment
import geotrellis.spark.io.AttributeStoreSpec

class HBaseAttributeStoreSpec extends AttributeStoreSpec with TestEnvironment {
  lazy val instance       = HBaseInstance(Seq("localhost"), "localhost")
  lazy val attributeStore = HBaseAttributeStore(instance, "attributes")
}
