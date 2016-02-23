package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._

class HadoopAttributeStoreSpec extends AttributeStoreSpec {
  lazy val attributeStore = HadoopAttributeStore(outputLocal)
}
