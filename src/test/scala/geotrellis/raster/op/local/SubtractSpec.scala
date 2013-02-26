package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SubtractSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Subtract") {
    it("subtracts two integers") { 
      run(Subtract(1,2)) should be (-1)
    }

    it("subtracts two doubles") {
      run(Subtract(.5,.3)) should be (.2)
    }
    
    it("subtracts a constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = run(Subtract(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) - 5)
        }
      }
    }

    it("subtracts a constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = run(Subtract(r,1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) - 1.0)
        }
      }
    }

    it("subtracts a constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = run(Subtract(10,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (10 - r.get(col,row))
        }
      }
    }

    it("subtracts a constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = run(Subtract(3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (3.0 - r.getDouble(col,row))
        }
      }
    }

    it("subtracts a double constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = run(Subtract(r,5.7))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (r.get(col,row) - 5.7).toInt)
        }
      }
    }

    it("subtracts a double constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = run(Subtract(r,0.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) - 0.1)
        }
      }
    }

    it("subtracts a double constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = run(Subtract(10.2,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (10.2 - r.get(col,row)).toInt )
        }
      }
    }

    it("subtracts a double constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = run(Subtract(3.3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (3.3 - r.getDouble(col,row))
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
  }
}
