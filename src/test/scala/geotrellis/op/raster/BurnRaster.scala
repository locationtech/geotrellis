package geotrellis.op

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import geotrellis.geometry.Polygon

import geotrellis.data.ColorBreaks
import geotrellis.Raster
import geotrellis.{Extent,RasterExtent}

import geotrellis.stat._
import geotrellis.op.raster.data.WarpRaster;
import geotrellis.op.raster._;
import geotrellis.process._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class WarpRasterSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("The WarpRaster operation") {
    val server = TestServer()

    val baseExtent = Extent(0.0, 0.0, 100.0, 100.0)
    val baseGeo = RasterExtent(baseExtent, 25, 25, 4, 4)
    val data = Array(1, 1, 1, 1,
                     2, 2, 2, 2,
                     3, 3, 3, 3,
                     4, 4, 4, 4)
    val raster = Raster(data, baseGeo)

    it("should load the full raster on its own extent") {
      val warpOp = WarpRaster(Literal(raster), baseGeo)
      val raster2 = server.run(warpOp)

      println(raster.rasterExtent)
      println(raster2.rasterExtent)

      println(raster.asciiDraw())
      println(raster2.asciiDraw())

      raster must be === raster2
    }
  }
}

