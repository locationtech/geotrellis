package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._
import geotrellis.source._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AndSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("And") {
    it("ands an Int raster and a constant") {
      assertEqual(And(createValueRaster(10,9),3), createValueRaster(10,1))
    }

    it("ands two Int rasters") {
      val op = And(createValueRaster(10,9),createValueRaster(10,3))
      assertEqual(op, 
                  createValueRaster(10,1))
    }

    it("ands a Double raster and a constant") {
      assertEqual(And(createValueRaster(10,9.4),3), createValueRaster(10,1.0))
    }

    it("ands two Double rasters") {
      assertEqual(And(createValueRaster(10,9.9),createValueRaster(10,3.2)), 
                  createValueRaster(10,1.0))
    }

    it("ands three tiled RasterDataSources correctly") {
      val rs1 = createRasterDataSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      val rs2 = createRasterDataSource(
        Array( 2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2),
        3,2,3,2)

      val rs3 = createRasterDataSource(
        Array( 3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3,

               3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3),
        3,2,3,2)

      getSource(rs1 & rs2 & rs3) match {
        case Complete(result,success) =>
          //println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (1^2^3)
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
