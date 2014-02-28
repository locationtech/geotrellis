package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._
import geotrellis.source._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class NotSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Not") {
    it("negates an Int raster") {
      assertEqual(Not(createValueRaster(10,9)), 
                  createValueRaster(10,-10))
    }

    it("negates a raster source") {
      val rs1 = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
          1,1,1, 1,1,1, 1,1,1,

          1,1,1, 1,1,1, 1,1,1,
          1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      run(~rs1) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (~1)
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
