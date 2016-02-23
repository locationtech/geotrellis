package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._

class FileAttributeStoreSpec extends AttributeStoreSpec {
  lazy val attributeStore = FileAttributeStore(outputLocalPath)
}
