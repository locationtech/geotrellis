package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._
import geotrellis.source._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class XorSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Xor") {
    it("xors an Int raster xor a constant") {
      assertEqual(Xor(createValueRaster(10,9),3), createValueRaster(10,10))
    }

    it("xors two Int rasters") {
      assertEqual(Xor(createValueRaster(10,9),createValueRaster(10,3)), 
                  createValueRaster(10,10))
    }

    it("xors a Double raster xor a constant") {
      assertEqual(Xor(createValueRaster(10,9.4),3), createValueRaster(10,10.0))
    }

    it("xors two Double rasters") {
      assertEqual(Xor(createValueRaster(10,9.9),createValueRaster(10,3.2)),
        createValueRaster(10,10.0))
    }

    it("xors three tiled RasterSources correctly") {
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

      getSource(rs1 ^ rs2 ^ rs3) match {
        case Complete(result,success) =>
//          println(success)
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
