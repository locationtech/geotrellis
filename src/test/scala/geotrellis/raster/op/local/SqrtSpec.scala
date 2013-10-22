package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._
import geotrellis.source._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SqrtSpec extends FunSpec 
                    with ShouldMatchers 
                    with TestServer 
                    with RasterBuilders {
  describe("Sqrt") {
    it("takes the square root of an integer raster") {
      val r = positiveIntegerRaster
      val result = run(Sqrt(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (math.sqrt(r.get(col,row)).toInt)
        }
      }
    }

    it("takes the square root of a double raster") {
      val r = probabilityRaster
      val result = run(Sqrt(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (math.sqrt(r.getDouble(col,row)))
        }
      }
    }

    it("takes the square root of a double raster source correctly") {
      val rs = RasterDataSource("mtsthelens_tiled")

      val r = runSource(rs)
      getSource(rs.localSqrt) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r.rasterExtent.rows / 3) {
            for(col <- 0 until r.rasterExtent.cols / 3) {
              val z = r.get(col,row)
              val rz = result.getDouble(col,row) 
              if(isNaN(z) || z < 0.0)
                isNaN(rz) should be (true)
              else 
                rz should be (math.sqrt(z) plusOrMinus 1e-5)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("takes the square root of a int raster source") {
      val rs = createRasterDataSource(
        Array( NODATA,9,9, 9,9,9, 9,9,9,
               9,9,9, 9,9,9, 9,9,9,

               9,9,9, 9,9,9, 9,9,9,
               9,9,9, 9,9,9, 9,9,9),
        3,2,3,2)

      getSource(rs.localSqrt) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (3)
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
