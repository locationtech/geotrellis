package geotrellis.spark.io.hbase

import geotrellis.spark.HBaseTestEnvironment
import geotrellis.spark.io.AttributeStoreSpec

class HBaseAttributeStoreSpec extends AttributeStoreSpec with HBaseTestEnvironment {
  lazy val instance       = HBaseInstance(Seq("localhost"), "localhost")
  lazy val attributeStore = HBaseAttributeStore(instance, "attributes")
}
