package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class AsinSpec extends FunSpec
                with ShouldMatchers
                with TestServer
                with RasterBuilders {
  describe("ArcSin") {
    it("finds arcsin of a double raster") {
      val rasterData = Array(
        0.0, 0.5, 1/math.sqrt(2),   math.sqrt(3)/2, 1.0, Double.NaN,   0, 0, 0,
        0.0,-0.5,-1/math.sqrt(2),  -math.sqrt(3)/2,-1.0, Double.NaN,   0, 0, 0,

        0.0, 0.5, 1/math.sqrt(2),   math.sqrt(3)/2, 1.0, Double.NaN,   0, 0, 0,
        0.0,-0.5,-1/math.sqrt(2),  -math.sqrt(3)/2,-1.0, Double.NaN,   0, 0, 0
      )
      val rs = createRasterSource(rasterData, 3, 2, 3, 2)
      val expectedAngles = List( 0.0,    1.0/6,  1.0/4,
                                 1.0/3,  0.5,    Double.NaN,
                                 0.0,    0.0,    0.0,

                                -0.0,   -1.0/6, -1.0/4,
                                -1.0/3, -0.5,   -Double.NaN,
                                -0.0,   -0.0,   -0.0,

                                 0.0,    1.0/6,  1.0/4,
                                 1.0/3,  0.5,    Double.NaN,
                                 0.0,    0.0,    0.0,
                                                             
                                -0.0,   -1.0/6, -1.0/4,
                                -1.0/3, -0.5,   -Double.NaN,
                                -0.0,   -0.0,   -0.0
                           ).map(x => math.Pi * x)
      run(rs.localAsin) match {
        case Complete(result, success) =>
          val width = 9
          val height = 4
          val len = expectedAngles.length
          for (y <- 0 until height) {
            for (x <- 0 until width) {
              val angle = result.getDouble(x, y)
              val i = (y*width + x) % len
              val expected = expectedAngles(i)
              val epsilon = math.ulp(angle)
              if (x == 5) {
                angle.isNaN should be (true)
              } else {
                angle should be (expected plusOrMinus epsilon)
              }
            }
          }
        case Error(msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
    it("is NaN when the absolute value of thecell of a double raster > 1") {
      val rasterData = Array(
         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, 2.6,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6,

         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, 2.6,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6
      )
      val rs = createRasterSource(rasterData, 3, 2, 3, 2)
      run(rs.localAsin) match {
        case Complete(result, success) =>
          for (y <- 0 until 4) {
            for (x <- 0 until 9) {
              result.getDouble(x, y).isNaN should be (true)
            }
          }
        case Error(msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
    it("finds arcsin of an int raster") {
      val rasterData = Array(
         0, 0, 0,   0, 0, 0,   0, 0, 0,
         1, 1, 1,   1, 1, 1,   1, 1, 1,
        -1,-1,-1,  -1,-1,-1,  -1,-1,-1,

         2, 2, 2,   2, 2, 2,   2, 2, 2,
        -2,-2,-2,  -2,-2,-2,  -2,-2,-2,
        NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA
      )
      val expectedAngles = Array(0.0, 0.5,-0.5,
                                 Double.NaN, Double.NaN,  Double.NaN)
                            .map(x => x * math.Pi)
      val rs = createRasterSource(rasterData, 3, 3, 3, 2)
      run(rs.localAsin) match {
        case Complete(result, success) =>
          for (y <- 0 until 3) {
            for (x <- 0 until 9) {
              val cellValue = result.getDouble(x, y)
              val epsilon = math.ulp(cellValue)
              val expected = expectedAngles(y)
              cellValue should be (expected plusOrMinus epsilon)
            }
          }
          for (y <- 3 until 6) {
            for (x <- 0 until 9) {
              result.getDouble(x, y).isNaN should be (true)
            }
          }
        case Error(msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("ArcSin on Raster") {
    it("finds arcsin of a double raster") {
      val rasterData = Array(
        0.0, 0.5, 1/math.sqrt(2),   math.sqrt(3)/2, 1.0, Double.NaN,   0, 0, 0,
        0.0,-0.5,-1/math.sqrt(2),  -math.sqrt(3)/2,-1.0, Double.NaN,   0, 0, 0,

        0.0, 0.5, 1/math.sqrt(2),   math.sqrt(3)/2, 1.0, Double.NaN,   0, 0, 0,
        0.0,-0.5,-1/math.sqrt(2),  -math.sqrt(3)/2,-1.0, Double.NaN,   0, 0, 0
      )
      val rs = createRaster(rasterData, 9, 4)
      val result = get(rs.localAsin())
      val expectedAngles = List( 0.0,    1.0/6,  1.0/4,
                                 1.0/3,  0.5,    Double.NaN,
                                 0.0,    0.0,    0.0,

                                -0.0,   -1.0/6, -1.0/4,
                                -1.0/3, -0.5,   -Double.NaN,
                                -0.0,   -0.0,   -0.0,

                                 0.0,    1.0/6,  1.0/4,
                                 1.0/3,  0.5,    Double.NaN,
                                 0.0,    0.0,    0.0,
                                                             
                                -0.0,   -1.0/6, -1.0/4,
                                -1.0/3, -0.5,   -Double.NaN,
                                -0.0,   -0.0,   -0.0
                           ).map(x => math.Pi * x)
      val width = 9
      val height = 4
      val len = expectedAngles.length
      for (y <- 0 until height) {
        for (x <- 0 until width) {
          val angle = result.getDouble(x, y)
          val i = (y*width + x) % len
          val expected = expectedAngles(i)
          val epsilon = math.ulp(angle)
          if (x == 5) {
            angle.isNaN should be (true)
          } else {
            angle should be (expected plusOrMinus epsilon)
          }
        }
      }
    }
    it("is NaN when the absolute value of thecell of a double raster > 1") {
      val rasterData = Array(
         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, 2.6,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6,

         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, 2.6,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6
      )
      val rs = createRaster(rasterData, 9, 4)
      val result = get(rs.localAsin())
      for (y <- 0 until 4) {
        for (x <- 0 until 9) {
          result.getDouble(x, y).isNaN should be (true)
        }
      }
    }
    it("finds arcsin of an int raster") {
      val rasterData = Array(
         0, 0, 0,   0, 0, 0,   0, 0, 0,
         1, 1, 1,   1, 1, 1,   1, 1, 1,
        -1,-1,-1,  -1,-1,-1,  -1,-1,-1,

         2, 2, 2,   2, 2, 2,   2, 2, 2,
        -2,-2,-2,  -2,-2,-2,  -2,-2,-2,
        NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA
      )
      val expectedAngles = Array(0.0, 0.5,-0.5,
                                 Double.NaN, Double.NaN,  Double.NaN)
                            .map(x => x * math.Pi)
      val rs = createRaster(rasterData, 9, 6)
      val result = get(rs.localAsin())
      for (y <- 0 until 3) {
        for (x <- 0 until 9) {
          val cellValue = result.getDouble(x, y)
          val epsilon = math.ulp(cellValue)
          val expected = expectedAngles(y)
          cellValue should be (expected plusOrMinus epsilon)
        }
      }
      for (y <- 3 until 6) {
        for (x <- 0 until 9) {
          result.getDouble(x, y).isNaN should be (true)
        }
      }
    }
  }
}
