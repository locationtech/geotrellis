package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._

class MockS3LayerWriter(
  attributeStore: AttributeStore,
  bucket: String,
  keyPrefix: String
) extends S3LayerWriter(attributeStore, bucket, keyPrefix) {
  override def rddWriter =
    new S3RDDWriter {
      def getS3Client = () => {
        new MockS3Client()
      }
    }
}
