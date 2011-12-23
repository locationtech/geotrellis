package trellis.operation

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import trellis.geometry.Polygon

import trellis.data.ColorBreaks
import trellis.raster.IntRaster
import trellis.{Extent,RasterExtent}

import trellis.stat._
import trellis.process._
import trellis.constant._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class BurnRasterSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("The BurnRaster operation") {
    val server = TestServer()
    server.start

    val baseExtent = Extent(0.0, 0.0, 100.0, 100.0)
    val baseGeo = RasterExtent(baseExtent, 25, 25, 4, 4)
    val data = Array(1, 1, 1, 1,
                     2, 2, 2, 2,
                     3, 3, 3, 3,
                     4, 4, 4, 4)
    val raster = IntRaster(data, 4, 4, baseGeo)

    it("should load the full raster on its own extent") {
      val op = BurnRaster(Literal(raster), baseGeo)
      val raster2 = server.run(op)

      println(raster.rasterExtent)
      println(raster2.rasterExtent)

      println(raster.asciiDraw())
      println(raster2.asciiDraw())

      raster.equals(raster2) must be === true
    }
  }
}

