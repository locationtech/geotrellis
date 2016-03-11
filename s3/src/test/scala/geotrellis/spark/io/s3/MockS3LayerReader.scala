package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.spark._
import spray.json._

class MockS3LayerReader(
  attributeStore: AttributeStore
)(implicit sc: SparkContext) extends S3LayerReader(attributeStore) {
  override def rddReader =
    new S3RDDReader {
      def getS3Client = () => new MockS3Client()
    }
}
