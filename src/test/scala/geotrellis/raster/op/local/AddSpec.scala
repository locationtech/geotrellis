package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AddSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Add") {
    it("adds two integers") { 
      run(Add(1,2)) should be (3)
    }

    it("adds two doubles") {
      run(Add(.2,.3)) should be (.5)
    }
    
    it("adds a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Add(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) + 5)
        }
      }
    }

    it("adds a constant value to each cell of another int value raster") {
      assertEqual(Add(6,createValueRaster(10,3)),createValueRaster(10,9))
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

    it("adds a double constant value to each cell of another int value raster") {
      assertEqual(Add(6,createValueRaster(10,3.3)),createValueRaster(10,9.3))
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

      val r1 = Raster(Array.fill(100)(3), re).defer
      val r2 = Raster(Array.fill(100)(6), re).defer
      val r3 = Raster(Array.fill(100)(9), re)

      assert(run(Add(r1, r2)) === r3)
    }
  }
  
  describe("AddArray") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val re = RasterExtent(e, 1.0, 1.0, 10, 10)

    def ri(n:Int) = Raster(Array.fill(100)(n), re)
    def rd(n:Double) = Raster(Array.fill(100)(n), re)

    def addInts(ns:Int*) = AddArray(ns.map(n => ri(n)).toArray.asInstanceOf[Array[Raster]])
    def addDoubles(ns:Double*) = AddArray(ns.map(n => rd(n)).toArray.asInstanceOf[Array[Raster]])

    it("adds integers") {
      val a = 3
      val b = 6
      val c = 9
      val n = NODATA

      assert(run(addInts(a, b)) === ri(c))
      assert(run(addInts(n, b)) === ri(b))
      assert(run(addInts(c, n)) === ri(c))
      assert(run(addInts(n, n)) === ri(n))
    }

    it("adds doubles") {
      val a = 3000000000.0
      val b = 6000000000.0
      val c = 9000000000.0
      val x = a + a + b + b + c
      val n = Double.NaN

      assert(run(addDoubles(a, b)) === rd(c))
      assert(run(addDoubles(n, b)) === rd(b))
      assert(run(addDoubles(c, n)) === rd(c))
      assert(run(addDoubles(n, n)) === rd(n))

      assert(run(addDoubles(a, a, b, b, c)) === rd(x))
    }
  }
}
