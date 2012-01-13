package trellis.data

import trellis.process.TestServer
import trellis.{Extent,RasterExtent}
import trellis.constant._
import trellis.raster._

import trellis.operation._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AsciiSpec extends Spec with MustMatchers with ShouldMatchers {
  val server = TestServer()

  val data:Array[Int] = (1 to 100).toArray

  val e = Extent(19.0, 9.0, 49.0, 39.0)
  val g = RasterExtent(e, 3.0, 3.0, 10, 10)
  val r = IntRaster(data, 10, 10, g)

  describe("An AsciiReader") {
    it ("should fail on non-existent files") {
      val path = "/does/not/exist.tif"
      evaluating { AsciiReader.read(path, None, None) } should produce [Exception]
    }

    it ("should write ASCII") {
      AsciiWriter.write("/tmp/foo.asc", r)
    }

    it ("should read ASCII") {
      val r2 = AsciiReader.read("/tmp/foo.asc", None, None)
      r2 must be === r
    }

    it ("should translate GeoTiff") {
      val r2 = GeoTiffReader.read("src/test/resources/econic.tif", None, None)
      AsciiWriter.write("/tmp/econic-trellis.asc", r2)
    }
  }
}
