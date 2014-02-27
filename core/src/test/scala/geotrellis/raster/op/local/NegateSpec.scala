package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._
import geotrellis.source._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class NegateSpec extends FunSpec 
                    with ShouldMatchers 
                    with TestServer 
                    with RasterBuilders {
  describe("Negate") {
    it("negates an integer raster") {
      val r = positiveIntegerRaster
      val result = get(Negate(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (-r.get(col,row))
        }
      }
    }

    it("negates a double raster") {
      val r = probabilityRaster
      val result = get(Negate(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-r.getDouble(col,row))
        }
      }
    }

    it("negates a double raster source correctly") {
      val rs = RasterSource("mtsthelens_tiled")

      val r = get(rs)
      run(-rs) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r.rasterExtent.rows / 3) {
            for(col <- 0 until r.rasterExtent.cols / 3) {
              val z = r.getDouble(col,row)
              val rz = result.getDouble(col,row)
              if(isNoData(z))
                isNoData(rz) should be (true)
              else 
                rz should be (-z)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("negates a int raster source") {
      val rs = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      run(-rs) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (-1)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
