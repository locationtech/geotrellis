package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SubtractSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Subtract") {
    it("subtracts a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Subtract(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) - 5)
        }
      }
    }

    it("subtracts a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Subtract(r,1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) - 1.0)
        }
      }
    }

    it("subtracts a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Subtract(r,5.7))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (r.get(col,row) - 5.7).toInt)
        }
      }
    }

    it("subtracts a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Subtract(r,0.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) - 0.1)
        }
      }
    }

    it("subtracts an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = run(Subtract(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (0)
        }
      }
    }
    
    it("subtracts a double raster to itself") {
      val r = probabilityRaster
      val result = run(Subtract(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (0.0)
        }
      }
    }

    it("should subtract 63 and 17 correctly") {
      val cols = 100
      val rows = 100

      val e = Extent(0.0, 0.0, 100.0, 100.0)
      val re = RasterExtent(e, e.width / cols, e.height / rows, cols, rows)

      def makeData(c:Int) = Array.fill(re.cols * re.rows)(c)
      def makeRaster(c:Int) = Raster(makeData(c), re)

      val r63 = makeRaster(63)
      val r46 = makeRaster(46)
      val r17 = makeRaster(17)

      val r = run(Subtract(r63, r17))
      r.get(0, 0) should be (r46.get(0, 0))
    }

    it("subtracts two tiled RasterSources correctly") {
      val rs1 = RasterSource("quad_tiled")
      val rs2 = RasterSource("quad_tiled2")

      val r1 = runSource(rs1)
      val r2 = runSource(rs2)
      getSource(rs1 - rs2) match {
        case Complete(result,success) =>
          //println(success)
          for(row <- 0 until r1.rasterExtent.rows) {
            for(col <- 0 until r1.rasterExtent.cols) {
              result.get(col,row) should be (r1.get(col,row) - r2.get(col,row))
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("subtracts three tiled RasterSources correctly") {
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

      getSource(rs1 - rs2 - rs3) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              result.get(col,row) should be (-4)
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
