package geotrellis.raster.op

import geotrellis.Raster
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._
import geotrellis.raster.BitConstant

/**
 * Created by jchien on 4/24/14.
 */
class ViewshedSpec extends FunSpec
                            with ShouldMatchers
                            with TestServer
                            with RasterBuilders {
  describe("Viewshed") {
    it("computes the viewshed of a flat int plane") {
      val r = createRaster(Array.fill(7*8)(1), 7, 8)
//      print(Viewshed.computeHeightRequired(3, 4, r).asciiDraw())
      assertEqual(Viewshed.computeViewable(3, 4, r), Raster(BitConstant(true, 7, 8), r.rasterExtent))
    }

    it("computes the viewshed of a flat double plane") {
      val r = createRaster(Array.fill(7*8)(1.5), 7, 8)
//      print(Viewshed.computeHeightRequired(3, 4, r).asciiDraw())
      assertEqual(Viewshed.computeViewable(3, 4, r), Raster(BitConstant(true, 7, 8), r.rasterExtent))
    }

    it("computes the viewshed of a double line") {
      val rasterData = Array (
        300.0, 1.0, 99.0, 0.0, 10.0, 200.0, 137.0
      )
      val viewable = Array (
        1, 0, 1, 1, 1, 1, 0
      )
      val r = createRaster(rasterData, 7, 1)
      val viewRaster = createRaster(viewable, 7, 1)
      print(Viewshed.computeHeightRequired(0, 3, r).asciiDraw())
      assertEqual(Viewshed.computeViewable(0, 3, r), viewRaster)
    }

    it("computes the viewshed of a double plane") {
      val rasterData = Array (
        999.0, 1.0,   1.0,   1.0,   1.0,   1.0,   999.0,
        1.0,   1.0,   1.0,   1.0,   1.0,   499.0, 1.0,
        1.0,   1.0,   1.0,   1.0,   99.0,  1.0,   1.0,
        1.0,   1.0,   999.0, 1.0,   1.0,   1.0,   1.0,
        1.0,   1.0,   1.0,   1.0,   100.0, 1.0,   1.0,
        1.0,   1.0,   1.0,   1.0,   1.0,   101.0, 1.0,
        1.0,   1.0,   1.0,   1.0,   1.0,   1.0,   102.0
      )
      val r = createRaster(rasterData, 7, 7)
      print(Viewshed.computeHeightRequired(3, 3, r).asciiDraw())
      print(Viewshed.computeViewable(3, 3, r).asciiDraw())
    }
    // Generates
    //    1     1     1     1     0     0     1
    //    0     1     1     1     0     1     0
    //    0     0     1     1     1     0     0
    //    0     0     1     1     1     1     1
    //    0     0     1     1     1     0     0
    //    0     1     1     1     0     0     0
    //    1     1     1     1     0     0     0

    it("computes the viewshed of a int plane") {
      val rasterData = Array (
        999, 1,   1,   1,   1,   499, 999,
        1,   1,   1,   1,   1,   499, 250,
        1,   1,   1,   1,   99,  1,   1,
        1,   999, 1,   1,   1,   1,   1,
        1,   1,   1,   1,   1,   1,   1,
        1,   1,   1,   0,   1,   1,   1,
        1,   1,   1,   1,   1,   1,   1
      )
      val r = createRaster(rasterData, 7, 7)
      print(Viewshed.computeHeightRequired(3, 3, r).asciiDraw())
      print(Viewshed.computeViewable(3, 3, r).asciiDraw())
    }
    // Generates
    //    1     1     1     1     0     1     1
    //    1     1     1     1     0     1     1
    //    0     1     1     1     1     0     0
    //    0     1     1     1     1     1     1
    //    0     1     1     1     1     1     1
    //    1     1     1     0     1     1     1
    //    1     1     1     1     1     1     1
  }
}
