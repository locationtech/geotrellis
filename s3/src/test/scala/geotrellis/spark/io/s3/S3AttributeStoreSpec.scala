package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._

class S3AttributeStoreSpec extends AttributeStoreSpec {
  val bucket = "attribute-store-test-mock-bucket"
  val prefix = "catalog"

  lazy val attributeStore = new S3AttributeStore(bucket, prefix) {
    override val s3Client = new MockS3Client()
  }
}
