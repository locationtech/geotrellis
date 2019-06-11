package geotrellis.spark.store.s3.util

import geotrellis.store.s3.util._
import geotrellis.util.RangeReader

import org.scalatest._

import java.net.URI


class S3RangeReaderProviderSpec extends FunSpec with Matchers {
  describe("S3RangeReaderProviderSpec") {
    val uri = new java.net.URI("s3://fake-bucket/some-prefix")

    it("should create a S3RangeReader from a URI") {
      val reader = RangeReader(uri)

      assert(reader.isInstanceOf[S3RangeReader])
    }
  }
}
