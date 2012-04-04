package geotrellis.data

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import Console.printf
import geotrellis.process.TestServer
import geotrellis.{Extent,RasterExtent}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class IntRasterReaderSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("An IntRasterReader") {
    it ("should work") {
      val e = Extent(-9.5, 3.8, 80 + -9.5, 80 + 3.8)
      val geo = RasterExtent(e, 8.0, 8.0, 10, 10)

      val server = TestServer()
      val raster = server.loadRaster("src/test/resources/quad8.arg", geo)

      val raster2 = IntRasterReader.read(raster, None)
      
      raster.equals(raster2) must be === true
      raster must be === raster2
    }
  }
}
