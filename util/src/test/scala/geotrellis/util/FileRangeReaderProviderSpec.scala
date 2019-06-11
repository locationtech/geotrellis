package geotrellis.util

import org.scalatest._

import java.net.URI


class FileRangeReaderProviderSpec extends FunSpec with Matchers {
  describe("FileRangeReaderProviderSpec") {
    val path = "raster/data/aspect.tif"

    it("should create a FileRangeReader from a URI") {
      val reader = RangeReader(new URI(path))

      assert(reader.isInstanceOf[FileRangeReader])
    }
  }
}
