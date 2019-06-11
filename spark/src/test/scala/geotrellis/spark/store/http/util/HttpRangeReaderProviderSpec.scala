package geotrellis.spark.store.http.util

import geotrellis.util.RangeReader

import org.scalatest._

import java.net.URI

class HttpRangeReaderProviderSpec extends FunSpec with Matchers {
  describe("HttpRangeReaderProviderSpec") {
    it("should create a HttpRangeReader from a URI") {
      val path = "http://localhost:8081/all-ones.tif"
      val reader = RangeReader(new URI(path))

      assert(reader.isInstanceOf[HttpRangeReader])
    }

    it("should dectect a bad URL") {
      val path = "httpa://localhost:8081/!!!!/all-ones.tif"
      val result = new HttpRangeReaderProvider().canProcess(new URI(path))

      result should be (false)
    }
  }
}
