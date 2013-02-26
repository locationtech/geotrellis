package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class MinSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Min") {
    it("mins two integers") {
      run(Min(3,2)) should be (2)
    }

    it("mins two doubles") {
      run(Min(.2,.3)) should be (.2)
    }
    
    it("mins a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Min(r,50))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val expected = if(r.get(col,row) < 50) { r.get(col,row) }
                         else { 50 }
          
          result.get(col,row) should be (expected)
        }
      }
    }

    it("mins a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Min(r,1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should not be (1.0)
        }
      }
    }

    it("mins a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = run(Min(r,40.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val expected = if(r.get(col,row) < 40.1) { r.get(col,row) }
                         else { 40 }

          result.get(col,row) should be (expected)
        }
      }
    }

    it("mins a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = run(Min(r,.3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val expected = if(r.getDouble(col,row) < .3) { r.getDouble(col,row) }
                         else { .3 }

          result.getDouble(col,row) should be (expected)
        }
      }
    }

    it("mins two integer rasters") {
      val r1 = createRaster(Array( -1,  2, -13, 5,
                                   -12, 7,  -3, 2,
                                   -8 , 6, -12, 7), 4,3)
      val r2 = createRaster(Array( 1,  -2, 13, -5,
                                   12, -7,  3, -2,
                                   8 , -6, 12, -7), 4,3)
      val result = run(Min(r1,r2))
      for(col <- 0 until 4) {
        for(row <- 0 until 3) {
          if(col % 2 != 1) {
            result.get(col,row) should be (r1.get(col,row))
          } else {
            result.get(col,row) should be (r2.get(col,row))
          }
        }
      }
    }

    it("mins two double rasters") {
      val r1 = createRaster(Array( -.1,  .25, -.13, .5,
                                   -.12, .7,  -.3, .2,
                                   -.8 , .6, -.12, .7), 4,3)
      val r2 = createRaster(Array( .1,  .2, .13, -.5,
                                   .12, -.7,  .3, -.2,
                                   .8 , -.6, .12, -.7), 4,3)
      val result = run(Min(r1,r2))
      for(col <- 0 until 4) {
        for(row <- 0 until 3) {
          if(col % 2 != 1) {
            result.get(col,row) should be (r1.get(col,row))
          } else {
            result.get(col,row) should be (r2.get(col,row))
          }
        }
      }
    }    
  }
}
