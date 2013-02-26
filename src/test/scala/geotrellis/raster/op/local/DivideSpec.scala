package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DivideSpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer 
                      with RasterBuilders {
  describe("Divide") {
    it("divides two integers") {
      run(Divide(9,2)) should be (4)
    }

    it("divides two doubles") {
      run(Divide(23.0,10.0)) should be (2.3)
    }
    
    it("divides a constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) / 5)
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = run(Divide(r,3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 3)
        }
      }
    }

    it("divides a constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(-10,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (-10 / r.get(col,row))
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = run(Divide(-3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.0 / r.getDouble(col,row))
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(r,5.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ((r.get(col,row) / 5.1).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = run(Divide(r,.3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 0.3)
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(-10.7,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (-10.7 / r.get(col,row)).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = run(Divide(-3.3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.3 / r.getDouble(col,row))
        }
      }
    }

    it("divides an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = run(Divide(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (1)
        }
      }
    }
    
    it("divides a double raster to itself") {
      val r = probabilityRaster
      val result = run(Divide(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (1.0)
        }
      }
    }
  }
}
