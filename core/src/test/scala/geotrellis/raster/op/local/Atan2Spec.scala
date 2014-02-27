package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class Atan2Spec extends FunSpec
                with ShouldMatchers
                with TestServer
                with RasterBuilders {
  describe("ArcTan2") {
    it("finds arctan2 of a double raster") {
      val rasterData1 = Array(
        0.0, 1.0, 1.0,
        math.sqrt(3), Double.PositiveInfinity, Double.NaN,

       -0.0,-1.0,-1.0,
       -math.sqrt(3), Double.NegativeInfinity, Double.NaN,

       -0.0,-1.0,-1.0,
       -math.sqrt(3), Double.NegativeInfinity, Double.NaN,

        0.0, 1.0, 1.0,
        math.sqrt(3), Double.PositiveInfinity, Double.NaN
      )
      val rasterData2 = Array(
        0.0, math.sqrt(3), 1.0,
        1.0, 1.0, 1.0,

        1.0, math.sqrt(3), 1.0,
        1.0, 1.0, 1.0,

       -1.0,-math.sqrt(3),-1.0,
       -1.0,-1.0,-1.0,

       -1.0,-math.sqrt(3),-1.0,
       -1.0,-1.0,-1.0
      )
      val rs1 = createRasterSource(rasterData1, 3, 2, 2, 2)
      val rs2 = createRasterSource(rasterData2, 3, 2, 2, 2)
      val expectedAngles = List( 0.0,    1.0/6,  1.0/4,
                                 1.0/3,  0.5,    Double.NaN,

                                -0.0,   -1.0/6, -1.0/4,
                                -1.0/3, -0.5,   -Double.NaN,

                                -1.0,   -5.0/6, -3.0/4,
                                -2.0/3, -0.5,    Double.NaN,

                                 1.0,    5.0/6,  3.0/4,
                                 2.0/3,  0.5,   -Double.NaN
                           )
                            .map(x => math.Pi * x)
      run(rs1.localAtan2(rs2)) match {
        case Complete(result, success) =>
          val width = 6
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
    it("finds arctan2 of an int raster") {
      val rasterData1 = Array(
         0, 0, 0,   0, 0, 0,   0, 0, 0,
         1, 1, 1,   1, 1, 1,   1, 1, 1,
        -1,-1,-1,  -1,-1,-1,  -1,-1,-1,

         2, 2, 2,   2, 2, 2,   2, 2, 2,
        -2,-2,-2,  -2,-2,-2,  -2,-2,-2,
        NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA
      )
      val rasterData2 = Array.fill(54)(1)
      val expectedAngles = Array(0.0, 0.25*math.Pi, -0.25*math.Pi,
                                 math.atan(2), math.atan(-2),  Double.NaN)
      val rs1 = createRasterSource(rasterData1, 3, 3, 3, 2)
      val rs2 = createRasterSource(rasterData2, 3, 3, 3, 2)
      run(rs1.localAtan2(rs2)) match {
        case Complete(result, success) =>
          for (y <- 0 until 5) {
            for (x <- 0 until 9) {
              val cellValue = result.getDouble(x, y)
              val epsilon = math.ulp(cellValue)
              val expected = expectedAngles(y)
              cellValue should be (expected plusOrMinus epsilon)
            }
          }
          for (y <- 5 until 6) {
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
    it("finds arctan2 of a double raster and an int raster") {
      val rasterData1 = Array[Double](
         0.0, 0.0, 0.0,   0.0, 0.0, 0.0,   0.0, 0.0, 0.0,
         1.0, 1.0, 1.0,   1.0, 1.0, 1.0,   1.0, 1.0, 1.0,
        -1.0,-1.0,-1.0,  -1.0,-1.0,-1.0,  -1.0,-1.0,-1.0,

         2.0, 2.0, 2.0,   2.0, 2.0, 2.0,   2.0, 2.0, 2.0,
        -2.0,-2.0,-2.0,  -2.0,-2.0,-2.0,  -2.0,-2.0,-2.0,
        Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN
      )
      val rasterData2 = Array.fill(54)(1)
      val expectedAngles = Array(0.0, 0.25*math.Pi, -0.25*math.Pi,
                                 math.atan(2), math.atan(-2),  Double.NaN)
      val rs1 = createRasterSource(rasterData1, 3, 3, 3, 2)
      val rs2 = createRasterSource(rasterData2, 3, 3, 3, 2)
      run(rs1.localAtan2(rs2)) match {
        case Complete(result, success) =>
          for (y <- 0 until 5) {
            for (x <- 0 until 9) {
              val cellValue = result.getDouble(x, y)
              val epsilon = math.ulp(cellValue)
              val expected = expectedAngles(y)
              cellValue should be (expected plusOrMinus epsilon)
            }
          }
          for (y <- 5 until 6) {
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
    it("finds arctan2 of a int raster and a double raster") {
      val rasterData1 = Array(
         0, 0, 0,   0, 0, 0,   0, 0, 0,
         1, 1, 1,   1, 1, 1,   1, 1, 1,
        -1,-1,-1,  -1,-1,-1,  -1,-1,-1,

         2, 2, 2,   2, 2, 2,   2, 2, 2,
        -2,-2,-2,  -2,-2,-2,  -2,-2,-2,
        NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA
      )
      val rasterData2 = Array.fill(54)(1.0)
      val expectedAngles = Array(0.0, 0.25*math.Pi, -0.25*math.Pi,
                                 math.atan(2), math.atan(-2),  Double.NaN)
      val rs1 = createRasterSource(rasterData1, 3, 3, 3, 2)
      val rs2 = createRasterSource(rasterData2, 3, 3, 3, 2)
      run(rs1.localAtan2(rs2)) match {
        case Complete(result, success) =>
          for (y <- 0 until 5) {
            for (x <- 0 until 9) {
              val cellValue = result.getDouble(x, y)
              val epsilon = math.ulp(cellValue)
              val expected = expectedAngles(y)
              cellValue should be (expected plusOrMinus epsilon)
            }
          }
          for (y <- 5 until 6) {
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
}
