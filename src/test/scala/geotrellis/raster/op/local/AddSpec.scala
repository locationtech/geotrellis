package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AddSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Add") {
    it("adds a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Add(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) + 5)
        }
      }
    }

    it("adds a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Add(r,1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) + 1.0)
        }
      }
    }

    it("adds a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Add(r,5.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (r.get(col,row) + 5.1).toInt)
        }
      }
    }

    it("adds a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Add(r,.3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) + 0.3)
        }
      }
    }

    it("adds an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = run(Add(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 2)
        }
      }
    }

    it("adds a double raster to itself") {
      val r = probabilityRaster
      val result = run(Add(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 2.0)
        }
      }
    }

    it("adds three rasters correctly") {
      val e = Extent(0.0, 0.0, 10.0, 10.0)
      val re = RasterExtent(e, 1.0, 1.0, 10, 10)

      val r1 = Raster(Array.fill(100)(3), re)
      val r2 = Raster(Array.fill(100)(6), re)
      val r3 = Raster(Array.fill(100)(9), re)

      assert(run(Add(r1, r2)) === r3)
    }

    it("adds two tiled RasterSources correctly") {
      val rs1 = RasterSource("quad_tiled")
      val rs2 = RasterSource("quad_tiled2")

      val r1 = runSource(rs1)
      val r2 = runSource(rs2)
      getSource(rs1 + rs2) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r1.rasterExtent.rows) {
            for(col <- 0 until r1.rasterExtent.cols) {
              result.get(col,row) should be (r1.get(col,row) + r2.get(col,row))
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("adds three tiled RasterSources correctly") {
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

      getSource(rs1 + rs2 + rs3) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
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
  
  describe("Add with sequences of rasters") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val re = RasterExtent(e, 1.0, 1.0, 10, 10)

    def ri(n:Int) = Raster(Array.fill(100)(n), re)
    def rd(n:Double) = Raster(Array.fill(100)(n), re)

    def addInts(ns:Int*) = Add(ns.map(n => ri(n)))
    def addDoubles(ns:Double*) = Add(ns.map(n => rd(n)))

    it("adds integers") {
      val a = 3
      val b = 6
      val c = 9
      val n = NODATA

      assertEqual(addInts(a, b),ri(c))
      assertEqual(addInts(n, b),ri(n))
      assertEqual(addInts(c, n),ri(n))
      assertEqual(addInts(n, n),ri(n))
    }

    it("adds doubles") {
      val a = 3000000000.0
      val b = 6000000000.0
      val c = 9000000000.0
      val x = a + a + b + b + c
      val n = Double.NaN

      assertEqual(addDoubles(a, b),rd(c))
      assertEqual(addDoubles(n, b),rd(n))
      assertEqual(addDoubles(c, n),rd(n))
      assertEqual(addDoubles(n, n),rd(n))

      assertEqual(addDoubles(a, a, b, b, c),rd(x))
    }
  }
}
