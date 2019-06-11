package geotrellis.spark.store.http.util

import geotrellis.util.RangeReader

import org.scalatest._

import java.net.URI

class HttpRangeReaderProviderSpec extends FunSpec with Matchers {
  describe("HttpRangeReaderProviderSpec") {
    val path = "http://localhost:8081/all-ones.tif"

    it("should create a HttpRangeReader from a URI") {
      val reader = RangeReader(new URI(path))

      assert(reader.isInstanceOf[HttpRangeReader])
    }
  }
}
