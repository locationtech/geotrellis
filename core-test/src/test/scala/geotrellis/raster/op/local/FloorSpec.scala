package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class FloorSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Floor") {    
    it("takes floor of int tiled RasterSource") {
      val rs = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      run(rs.localFloor) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
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

    it("takes floor of Double tiled RasterSource") {
      val rs = createRasterSource(
        Array( Double.NaN,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3,
               1.3,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3,

               1.3,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3,
               1.3,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3),
        3,2,3,2)

      run(rs.localFloor) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                isNoData(result.getDouble(col,row)) should be (true)
              else
                result.getDouble(col,row) should be (1.0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("Floor on Raster") {
    it("takes floor of int Raster") {
      val r = createRaster(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        9, 4)

      val result =get(r.localFloor())

      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if(row == 0 && col == 0)
            result.get(col,row) should be (NODATA)
          else
            result.get(col,row) should be (1)
        }
      }
    }

    it("takes floor of Double Raster") {
      val r = createRaster(
        Array( Double.NaN,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3,
               1.3,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3,

               1.3,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3,
               1.3,1.3,1.3, 1.3,1.3,1.3, 1.3,1.3,1.3),
        9, 4)

      val result = get(r.localFloor())

      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if(row == 0 && col == 0)
            isNoData(result.getDouble(col,row)) should be (true)
          else
            result.getDouble(col,row) should be (1.0)
        }
      }
    }
  }
}
