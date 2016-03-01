package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.utils.cache._

import org.apache.spark._
import spray.json._

class MockS3LayerReader(
  attributeStore: AttributeStore[JsonFormat],
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None
)(implicit sc: SparkContext) extends S3LayerReader(attributeStore, getCache) {
  override def rddReader =
    new S3RDDReader {
      def getS3Client = () => new MockS3Client()
    }
}
