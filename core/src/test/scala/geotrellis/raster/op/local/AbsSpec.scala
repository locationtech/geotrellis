package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class AbsSpec extends FunSpec
                with ShouldMatchers
                with TestServer
                with RasterBuilders {
  describe("Abs") {
    it("takes the absolute value of each cell of an int tiled raster") {
      val rasterData = Array(
         1,-1, 1,  -1, 1,-1,  1,-1, NODATA,
        -1, 1,-1,   1,-1, 1, -1, 1,-1,

         1,-1, 1,  -1, 1,-1,  1,-1, 1,
        -1, 1,-1,   1,-1, 1, -1, 1,-1
      )
      val rs = createRasterSource(rasterData, 3, 2, 3, 2)

      run(rs.localAbs) match {
        case Complete(result, success) =>
          for (y <- 0 until 4) {
            for (x <- 0 until 9) {
              if (x == 8 && y == 0)
                result.get(x, y) should be (NODATA)
              else
                result.get(x, y) should be (1)
            }
          }
        case Error(msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
    it("takes the absolute value of each cell of a double tiled raster") {
      val rasterData = Array(
         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, 2.6,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6,

         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, Double.NaN,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6
      )
      val rs = createRasterSource(rasterData, 3, 2, 3, 2)
      run(rs.localAbs) match {
        case Complete(result, success) =>
          for (y <- 0 until 4) {
            for (x <- 0 until 9) {
              if (x == 8 && y == 2)
                result.getDouble(x, y).isNaN should be (true)
              else
                result.getDouble(x, y) should be (2.6)
            }
          }
        case Error(msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
