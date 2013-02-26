package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import scala.math.min

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ConditionalSpec extends FunSpec 
                    with ShouldMatchers 
                    with TestServer 
                    with RasterBuilders {
  describe("IfCell") {
    it("should work with integers") {
      val r = positiveIntegerRaster
      val result = run(IfCell(r,{z:Int => z > 6}, 6))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          result.get(col,row) should be (min(r.get(col,row),6))
        }
      }
    }

    it("should work with doubles") {
      val r = probabilityRaster
      val result = run(IfCell(r,{z:Double => z > 0.5}, 1.0))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z = r.getDouble(col,row)
          result.getDouble(col,row) should be (if (z > 0.5) { 1.0 } else { z })
        }
      }
    }

    it("should work with integer function on TypeDouble raster for NoData values") {
      val r = probabilityNoDataRaster
      val result = run(IfCell(r,{z:Int => z == NODATA}, -1000))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
        }
      }
    }

    it("should work with double function on TypeInt raster for NoData values") {
      val r = positiveIntegerNoDataRaster
      val result = run(IfCell(r,{z:Double => java.lang.Double.isNaN(z)}, -1000.0))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
        }
      }
    }
  }

  describe("IfElseCell") {
    it("should work with integers") {
      val r = positiveIntegerRaster
      val result = run(IfCell(r, {z:Int => z > 6}, 6, 2))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          result.get(col,row) should be (if (r.get(col,row) > 6) { 6 } else { 2 })
        }
      }
    }

    it("should work with doubles") {
      val r = probabilityRaster
      val result = run(IfCell(r, {z:Double => z < .5}, 0.01, .99))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z = r.getDouble(col,row)
          result.getDouble(col,row) should be (if (z < 0.5) { 0.01 } else { .99 })
        }
      }
    }

    it("should work with integer function on TypeDouble raster for NoData values") {
      val r = probabilityNoDataRaster
      val result = run(IfCell(r,{z:Int => z == NODATA}, -1000, 2000))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
          else { result.getDouble(col,row) should be (2000.0) }
        }
      }
    }

    it("should work with double function on TypeInt raster for NoData values") {
      val r = positiveIntegerNoDataRaster
      val result = run(IfCell(r,{z:Double => java.lang.Double.isNaN(z)}, -1000.0, 2000.0))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
          else { result.get(col,row) should be (2000) }
        }
      }
    }
  }

  describe("BinaryIfCell") {
    it("should work with integers") {
      val r1 = createRaster(Array( -1,  2, -13, 5,
                                   -12, 7,  -3, 2,
                                   -8 , 6, -12, 7), 4,3)
      val r2 = createRaster(Array( 1,  -2, 13, -5,
                                   12, -7,  3, -2,
                                   8 , -6, 12, -7), 4,3)
      val result = run(IfCell(r1,r2, {(z1:Int,z2:Int) => z1 > z2}, 80))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.get(col,row)
          val z2 = r2.get(col,row)
          val expected = if (z1 > z2) { 80 } else { z1 }
          result.get(col,row) should be (expected)
        }
      }
    }

    it("should work with doubles") {
      val r1 = createRaster(Array( -.1,  .25, -.13, .5,
                                   -.12, .7,  -.3, .2,
                                   -.8 , .6, -.12, .7), 4,3)
      val r2 = createRaster(Array( .1,  .2, .13, -.5,
                                   .12, -.7,  .3, -.2,
                                   .8 , -.6, .12, -.7), 4,3)
      val result = run(IfCell(r1,r2, {(z1:Double,z2:Double) => z1 > z2}, -0.5))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.getDouble(col,row)
          val z2 = r2.getDouble(col,row)
          val expected = if (z1 > z2) { -0.5 } else { z1 }
          result.getDouble(col,row) should be (expected)
        }
      }
    }

    it("should work with integer function on TypeDouble raster for NoData values") {
      val r1 = createRaster(Array( -.1,  Double.NaN, -.13, .5,
                                   -.12, .7,  -.3, Double.NaN,
                                   -.8 , Double.NaN, -.12, .7), 4,3)
      val r2 = createRaster(Array( .1,  .2, .13, Double.NaN,
                                   .12, Double.NaN,  .3, -.2,
                                   .8 , -.6, .12, Double.NaN), 4,3)
      val r = probabilityNoDataRaster
      val result = run(IfCell(r1,r2,{(z1:Int,z2:Int) => z1 == NODATA || z2 == NODATA }, -1000))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
        }
      }
    }

    it("should work with double function on TypeInt raster for NoData values") {
      val r1 = createRaster(Array( -1,  NODATA, -13, 5,
                                   -12, 7,  -3, NODATA,
                                   -8 , NODATA, -12, 7), 4,3)
      val r2 = createRaster(Array( 1,  -2, 13, NODATA,
                                   12, NODATA,  3, -2,
                                   8 , -6, 12, NODATA), 4,3)
      val r = positiveIntegerNoDataRaster
      val result = run(IfCell(r1,r2,{ (z1:Double,z2:Double) => 
        java.lang.Double.isNaN(z1) || java.lang.Double.isNaN(z2)
      }, -1000.0))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
        }
      }
    }
  }

  describe("BinaryIfElseCell") {
    it("should work with integers") {
      val r1 = createRaster(Array( -1,  2, -13, 5,
                                   -12, 7,  -3, 2,
                                   -8 , 6, -12, 7), 4,3)
      val r2 = createRaster(Array( 1,  -2, 13, -5,
                                   12, -7,  3, -2,
                                   8 , -6, 12, -7), 4,3)
      val result = run(IfCell(r1,r2, {(z1:Int,z2:Int) => z1 > z2}, 80,-20))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.get(col,row)
          val z2 = r2.get(col,row)
          val expected = if (z1 > z2) { 80 } else { -20 }
          result.get(col,row) should be (expected)
        }
      }
    }

    it("should work with doubles") {
      val r1 = createRaster(Array( -.1,  .25, -.13, .5,
                                   -.12, .7,  -.3, .2,
                                   -.8 , .6, -.12, .7), 4,3)
      val r2 = createRaster(Array( .1,  .2, .13, -.5,
                                   .12, -.7,  .3, -.2,
                                   .8 , -.6, .12, -.7), 4,3)
      val result = run(IfCell(r1,r2, {(z1:Double,z2:Double) => z1 > z2}, 1.1,0.7))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.getDouble(col,row)
          val z2 = r2.getDouble(col,row)
          val expected = if (z1 > z2) { 1.1 } else { 0.7 }
          result.getDouble(col,row) should be (expected)
        }
      }
    }

    it("should work with integer function on TypeDouble raster for NoData values") {
      val r1 = createRaster(Array( -.1,  Double.NaN, -.13, .5,
                                   -.12, .7,  -.3, Double.NaN,
                                   -.8 , Double.NaN, -.12, .7), 4,3)
      val r2 = createRaster(Array( .1,  .2, .13, Double.NaN,
                                   .12, Double.NaN,  .3, -.2,
                                   .8 , -.6, .12, Double.NaN), 4,3)
      val r = probabilityNoDataRaster
      val result = run(IfCell(r1,r2,{(z1:Int,z2:Int) => z1 == NODATA || z2 == NODATA }, -1000,2000))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
          else { result.getDouble(col,row) should be (2000.0) }
        }
      }
    }

    it("should work with double function on TypeInt raster for NoData values") {
      val r1 = createRaster(Array( -1,  NODATA, -13, 5,
                                   -12, 7,  -3, NODATA,
                                   -8 , NODATA, -12, 7), 4,3)
      val r2 = createRaster(Array( 1,  -2, 13, NODATA,
                                   12, NODATA,  3, -2,
                                   8 , -6, 12, NODATA), 4,3)
      val r = positiveIntegerNoDataRaster
      val result = run(IfCell(r1,r2,{ (z1:Double,z2:Double) => 
        java.lang.Double.isNaN(z1) || java.lang.Double.isNaN(z2)
      }, -1000.0,2000.0))
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
          else { result.get(col,row) should be (2000) }
        }
      }
    }
  }
}
