package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class OrSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Or") {
    it("ors an Int raster or a constant") {
      assertEqual(Or(createValueRaster(10,9),3), createValueRaster(10,11))
    }

    it("ors two Int rasters") {
      assertEqual(Or(createValueRaster(10,9),createValueRaster(10,3)), 
                  createValueRaster(10,11))
    }

    it("ors a Double raster or a constant") {
      assertEqual(Or(createValueRaster(10,9.4),3), createValueRaster(10,11.0))
    }

    it("ors two Double rasters") {
      assertEqual(Or(createValueRaster(10,9.9),createValueRaster(10,3.2)), 
                  createValueRaster(10,11.0))
    }

    it("ors three tiled RasterSources correctly") {
      val rs1 = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      val rs2 = createRasterSource(
        Array( 2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2),
        3,2,3,2)

      val rs3 = createRasterSource(
        Array( 3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3,

               3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3),
        3,2,3,2)

      run(rs1 | rs2 | rs3) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (1|2|3)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("Or on Raster") {
    it("ors an Int raster or a constant") {
      assertEqual((createValueRaster(10,9) | 3), createValueRaster(10,11))
    }

    it("ors two Int rasters") {
      assertEqual((createValueRaster(10,9) | createValueRaster(10,3)),
                  createValueRaster(10,11))
    }

    it("ors a Double raster or a constant") {
      assertEqual((createValueRaster(10,9.4) | 3), createValueRaster(10,11.0))
    }

    it("ors two Double rasters") {
      assertEqual((createValueRaster(10,9.9) | createValueRaster(10,3.2)),
                  createValueRaster(10,11.0))
    }

    it("ors a sequence of rasters") {
      val r1 = createRaster(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        9, 4)

      val r2 = createRaster(
        Array( 2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2),
        9, 4)

      val r3 = createRaster(
        Array( 3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3,

               3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3),
        9, 4)

      val s = Seq(r2, r3)
      val result = get(r1 | s)

      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if(row == 0 && col == 0)
            result.get(col,row) should be (NODATA)
          else
            result.get(col,row) should be (1|2|3)
        }
      }
    }
  }
}
