package geotrellis.raster.op.local

import geotrellis._
import geotrellis.testutil._
import geotrellis.data._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ColorRasterSpec extends FunSpec with ShouldMatchers 
                                   with TestServer 
                                   with RasterBuilders {
  describe("ColorRaster - Integers") {
    val n = NODATA

    it("should map NODATA correctly") {
      val map = Map( 1 -> 1, 2 -> 2)
      val r = createRaster(Array(
        1, n, n, 1,
        2, n, n, 2,
        n, 3, 3, n
      ), 4, 3)

      val result = run(ColorRaster(r,map,ColorMapOptions(LessThan,5)))

      result.get(1,0) should be (5)
      result.get(2,0) should be (5)
      result.get(1,1) should be (5)
      result.get(2,1) should be (5)
      result.get(0,2) should be (5)
      result.get(3,2) should be (5)
    }

    it("should map colors correctly with GreaterThan") {
      val map = Map( 
        10 -> 1,
        20 -> 2,
        30 -> 3,
        40 -> 4
      )

      val r = createRaster(Array(
        1, 11, 21, 31,41,
        2, 12, 22, 32,42,
        3, 13, 23, 33,43,
        4, 14, 24, 34,44,
        5, 15, 25, 35,45,
        6, 16, 26, 36,46

      ), 5, 6)

      val result = run(ColorRaster(r,map,ColorMapOptions(GreaterThan,5, noMapColor = 7)))

      for(i <- 0 to 5) { result.get(0,i) should be(7) }
      for(i <- 0 to 5) { result.get(1,i) should be(1) }
      for(i <- 0 to 5) { result.get(2,i) should be(2) }
      for(i <- 0 to 5) { result.get(3,i) should be(3) }
      for(i <- 0 to 5) { result.get(4,i) should be(4) }
    }

    it("should map colors correctly with LessThan") {
      val map = Map( 
        10 -> 1,
        20 -> 2,
        30 -> 3,
        40 -> 4
      )

      val r = createRaster(Array(
        1, 11, 21, 31,41,
        2, 12, 22, 32,42,
        3, 13, 23, 33,43,
        4, 14, 24, 34,44,
        5, 15, 25, 35,45,
        6, 16, 26, 36,46

      ), 5, 6)

      val result = run(ColorRaster(r,map,ColorMapOptions(LessThan,5,noMapColor = 7)))

      for(i <- 0 to 5) { result.get(0,i) should be(1) }
      for(i <- 0 to 5) { result.get(1,i) should be(2) }
      for(i <- 0 to 5) { result.get(2,i) should be(3) }
      for(i <- 0 to 5) { result.get(3,i) should be(4) }
      for(i <- 0 to 5) { result.get(4,i) should be(7) }
    }

    it("should map colors correctly with Exact, not strict") {
      val map = Map( 
        10 -> 1,
        20 -> 2,
        30 -> 3,
        40 -> 4
      )

      val r = createRaster(Array(
        10, 20, 30, 40, 41,
        10, 20, 30, 40, 42,
        10, 20, 31, 40, 43,
        10, 20, 30, 40, 44
      ), 5, 4)

      val result = run(ColorRaster(r,map,ColorMapOptions(Exact,5,noMapColor = 7)))

      for(i <- 0 to 3) { result.get(0,i) should be(1) }
      for(i <- 0 to 3) { result.get(1,i) should be(2) }
      for(i <- 0 to 3) { 
        if(i != 2) { result.get(2,i) should be(3) }
        else { result.get(2,i) should be(7) }
      }
      for(i <- 0 to 3) { result.get(3,i) should be(4) }
      for(i <- 0 to 3) { result.get(4,i) should be(7) }
    }

    it("should throw exception when strict and not mapped value") {
      val map = Map( 
        10 -> 1,
        20 -> 2,
        30 -> 3,
        40 -> 4
      )

      val r = createRaster(Array(
        10, 20, 30, 40, 
        10, 20, 30, 40, 
        10, 20, 31, 40
      ), 4, 3)

      intercept[Exception] {
        run(ColorRaster(r,map,ColorMapOptions(Exact,5,noMapColor = 7, strict = true)))
         .toArray
      }
    }
  }

  describe("ColorRaster - Doubles") {
    val n = Double.NaN

    it("should map NODATA correctly") {
      val map = Map( 0.1 -> 1, 0.2 -> 2)
      val r = createRaster(Array(
        0.1, n, n, 0.1,
        0.2, n, n, 0.2,
        n, 0.3, 0.3, n
      ), 4, 3)

      val result = run(ColorRaster(r,map,ColorMapOptions(LessThan,5)))
      result.get(1,0) should be (5)
      result.get(2,0) should be (5)
      result.get(1,1) should be (5)
      result.get(2,1) should be (5)
      result.get(0,2) should be (5)
      result.get(3,2) should be (5)
    }

    it("should map colors correctly with GreaterThan") {
      val map = Map( 
        1.0 -> 1,
        2.0 -> 2,
        3.0 -> 3,
        4.0 -> 4
      )

      val r = createRaster(Array(
        0.1, 1.1, 2.1, 3.1,4.1,
        0.2, 1.2, 2.2, 3.2,4.2,
        0.3, 1.3, 2.3, 3.3,4.3,
        0.4, 1.4, 2.4, 3.4,4.4,
        0.5, 1.5, 2.5, 3.5,4.5,
        0.6, 1.6, 2.6, 3.6,4.6

      ), 5, 6)

      val result = run(ColorRaster(r,map,ColorMapOptions(GreaterThan,5, noMapColor = 7)))

      for(i <- 0 to 5) { result.get(0,i) should be(7) }
      for(i <- 0 to 5) { result.get(1,i) should be(1) }
      for(i <- 0 to 5) { result.get(2,i) should be(2) }
      for(i <- 0 to 5) { result.get(3,i) should be(3) }
      for(i <- 0 to 5) { result.get(4,i) should be(4) }
    }

    it("should map colors correctly with LessThan") {
      val map = Map( 
        1.0 -> 1,
        2.0 -> 2,
        3.0 -> 3,
        4.0 -> 4
      )

      val r = createRaster(Array(
        0.1, 1.1, 2.1, 3.1,4.1,
        0.2, 1.2, 2.2, 3.2,4.2,
        0.3, 1.3, 2.3, 3.3,4.3,
        0.4, 1.4, 2.4, 3.4,4.4,
        0.5, 1.5, 2.5, 3.5,4.5,
        0.6, 1.6, 2.6, 3.6,4.6

      ), 5, 6)

      val result = run(ColorRaster(r,map,ColorMapOptions(LessThan,5,noMapColor = 7)))

      for(i <- 0 to 5) { result.get(0,i) should be(1) }
      for(i <- 0 to 5) { result.get(1,i) should be(2) }
      for(i <- 0 to 5) { result.get(2,i) should be(3) }
      for(i <- 0 to 5) { result.get(3,i) should be(4) }
      for(i <- 0 to 5) { result.get(4,i) should be(7) }
    }

    it("should map colors correctly with Exact, not strict") {
      val map = Map( 
        1.0 -> 1,
        2.0 -> 2,
        3.0 -> 3,
        4.0 -> 4
      )

      val r = createRaster(Array(
        1.0, 2.0, 3.0, 4.0, 4.1,
        1.0, 2.0, 3.0, 4.0, 4.2,
        1.0, 2.0, 3.1, 4.0, 4.3,
        1.0, 2.0, 3.0, 4.0, 4.4
      ), 5, 4)

      val result = run(ColorRaster(r,map,ColorMapOptions(Exact,5,noMapColor = 7)))

      for(i <- 0 to 3) { result.get(0,i) should be(1) }
      for(i <- 0 to 3) { result.get(1,i) should be(2) }
      for(i <- 0 to 3) { 
        if(i != 2) { result.get(2,i) should be(3) }
        else { result.get(2,i) should be(7) }
      }
      for(i <- 0 to 3) { result.get(3,i) should be(4) }
      for(i <- 0 to 3) { result.get(4,i) should be(7) }
    }

    it("should throw exception when strict and not mapped value") {
      val map = Map( 
        1.0 -> 1,
        2.0 -> 2,
        3.0 -> 3,
        4.0 -> 4
      )

      val r = createRaster(Array(
        1.0, 2.0, 3.0, 4.0, 
        1.0, 2.0, 3.0, 4.0, 
        1.0, 2.0, 3.1, 4.0
      ), 4, 3)

      intercept[Exception] {
        run(ColorRaster(r,map,ColorMapOptions(Exact,5,noMapColor = 7, strict = true)))
         .toArray
      }
    }
  }
}
