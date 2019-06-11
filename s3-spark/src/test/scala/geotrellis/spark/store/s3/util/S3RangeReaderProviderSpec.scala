package geotrellis.spark.store.s3.util

import geotrellis.spark.store.s3.S3TestUtils
import geotrellis.store.s3.util._
import geotrellis.spark.store.s3.testkit.MockS3Client
import geotrellis.store.s3.S3ClientProducer
import geotrellis.util.RangeReader

import org.scalatest._

class S3RangeReaderProviderSpec extends FunSpec with Matchers {
  val client = MockS3Client()
  S3TestUtils.cleanBucket(client, "fake-bucket")
  S3ClientProducer.set(() => client)

  describe("S3RangeReaderProviderSpec") {
    val uri = new java.net.URI("s3://fake-bucket/some-prefix")

    it("should create a S3RangeReader from a URI") {
      val reader = RangeReader(uri)

      assert(reader.isInstanceOf[S3RangeReader])
    }
  }
}
