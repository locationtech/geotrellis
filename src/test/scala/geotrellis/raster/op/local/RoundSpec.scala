package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RoundSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Round") {    
    it("takes log of int tiled RasterSource") {
      val rs = createRasterSource(
        Array( NODATA,20,20, 20,20,20, 20,20,20,
               20,20,20, 20,20,20, 20,20,20,

               20,20,20, 20,20,20, 20,20,20,
               20,20,20, 20,20,20, 20,20,20),
        3,2,3,2)

      getSource(rs.localRound) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (20)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("takes log of Double tiled RasterSource") {
      val rs = createRasterSource(
        Array( Double.NaN,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2,
               34.2,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2,

               34.2,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2,
               34.2,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2),
        3,2,3,2)

      getSource(rs.localRound) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                isNaN(result.getDouble(col,row)) should be (true)
              else
                result.getDouble(col,row) should be (34.0)
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
