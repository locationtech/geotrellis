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
  }
}
