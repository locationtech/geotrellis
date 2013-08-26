package geotrellis.raster.op.zonal

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

import scala.collection.mutable

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ZonalClassificationSpec extends FunSpec 
                             with ShouldMatchers 
                             with TestServer 
                             with RasterBuilders {
  describe("ZonalClassification") {
    it("correctly reclassifies") { 
      val r = createRaster(
        Array(1, 2, 2, 2, 3, 1, 6, 5, 1,
              1, 2, 2, 2, 3, 6, 6, 5, 5,
              1, 3, 5, 3, 6, 6, 6, 5, 5,
              3, 1, 5, 6, 6, 6, 6, 6, 2,
              7, 7, 5, 6, 1, 3, 3, 3, 2,
              7, 7, 5, 5, 5, 4, 3, 4, 2,
              7, 7, 5, 5, 5, 4, 3, 4, 2,
              7, 2, 2, 5, 4, 4, 3, 4, 4),
        9,8)

      val zones = createRaster(
        Array(1, 1, 1, 4, 4, 4, 5, 6, 6,
              1, 1, 1, 4, 4, 5, 5, 6, 6,
              1, 1, 2, 4, 5, 5, 5, 6, 6,
              1, 2, 2, 3, 3, 3, 3, 3, 3,
              2, 2, 2, 3, 3, 3, 3, 3, 3,
              2, 2, 2, 7, 7, 7, 7, 8, 8,
              2, 2, 2, 7, 7, 7, 7, 8, 8,
              2, 2, 2, 7, 7, 7, 7, 8, 8),
        9,8)

      var result = run(ZonalClassification(r,zones)(_ + 10))

      val (cols,rows) = (result.rasterExtent.cols, result.rasterExtent.rows)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          val zone = zones.get(col,row)
          val value = result.get(col,row)
          value should be (zone + 10)
        }
      }
    }
  }
}
