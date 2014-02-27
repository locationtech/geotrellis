package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._
import geotrellis.source._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class DefinedSpec extends FunSpec 
                    with ShouldMatchers 
                    with TestServer 
                    with RasterBuilders {
  describe("Defined") {
    it("returns correct result for an integer raster") {
      val r = positiveIntegerNoDataRaster
      val result = get(Defined(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.get(col,row))) result.get(col,row) should be (0)
          else result.get(col,row) should be (1)
        }
      }
    }

    it("returns correct result for a double raster") {
      val r = probabilityNoDataRaster
      val result = get(Defined(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.getDouble(col,row))) result.get(col,row) should be (0)
          else result.get(col,row) should be (1)
        }
      }
    }

    it("returns correct result for a double raster source correctly") {
      val rs = RasterSource("mtsthelens_tiled")

      val r = get(rs)
      run(rs.localDefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r.rasterExtent.rows / 3) {
            for(col <- 0 until r.rasterExtent.cols / 3) {
              val z = r.getDouble(col,row)
              val rz = result.get(col,row)
              if(isNoData(z))
                rz should be (0)
              else 
                rz should be (1)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("returns correct result for a int raster source") {
      val rs = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      run(rs.localDefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (0)
              else
                result.get(col,row) should be (1)
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
