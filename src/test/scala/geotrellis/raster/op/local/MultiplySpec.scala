package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class MultiplySpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer 
                      with RasterBuilders {
  describe("Multiply") {
    it("multiplys a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Multiply(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 5)
        }
      }
    }

    it("multiplys a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Multiply(r,3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 3.0)
        }
      }
    }

    it("multiplys a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Multiply(r,5.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ((r.get(col,row) * 5.1).toInt)
        }
      }
    }

    it("multiplys a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Multiply(r,.3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 0.3)
        }
      }
    }

    it("multiplys an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = run(Multiply(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (math.pow(r.get(col,row),2.0))
        }
      }
    }
    
    it("multiplys a double raster to itself") {
      val r = probabilityRaster
      val result = run(Multiply(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (math.pow(r.getDouble(col,row), 2.0))
        }
      }
    }

    it("multiplies two tiled RasterSources correctly") {
      val rs1 = RasterSource("quad_tiled")
      val rs2 = RasterSource("quad_tiled2")

      val r1 = runSource(rs1)
      val r2 = runSource(rs2)
      getSource(rs1 * rs2) match {
        case Complete(result,success) =>
          //println(success)
          for(row <- 0 until r1.rasterExtent.rows) {
            for(col <- 0 until r1.rasterExtent.cols) {
              result.get(col,row) should be (r1.get(col,row) * r2.get(col,row))
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("multiplies three tiled RasterSources correctly") {
      val rs1 = createRasterSource(
        Array( 1,1,1, 1,1,1, 1,1,1,
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

      getSource(rs1 * rs2 * rs3) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              result.get(col,row) should be (6)
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
