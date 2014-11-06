package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class BilinearInterpolationSpec extends FunSpec with Matchers {

  val Epsilon = 1e-9

  def testInterpolationInt(
    x: Double,
    y: Double,
    res: Double,
    tile: Tile = ArrayTile(Array[Int](1, 2, 3, 4), 2, 2),
    extent: Extent = Extent(0, 0, 1, 1)) = {
    val bi = new BilinearInterpolation(tile, extent)
    bi.interpolate(x, y) should be (res)
  }

  def testInterpolationDouble(
    x: Double,
    y: Double,
    res: Double,
    tile: Tile = ArrayTile(Array[Double](1, 2, 3, 4), 2, 2),
    extent: Extent = Extent(0, 0, 1, 1)) = {
    val bi = new BilinearInterpolation(tile, extent)
    bi.interpolateDouble(x, y) should be (res +- Epsilon)
  }

  def testBilinearSquareCenter(tile: Tile, result: Double) =
    testInterpolationDouble(0.5, 0.5, result, tile)

  describe("interpolates correctly at square center") {

    it("should interpolate correctly when all values same") {
      val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
      testBilinearSquareCenter(tile, 100)
    }

    it("should interpolate correctly with one different value and three same") {
      val tiles = List(
        ArrayTile(Array[Int](200, 100, 100, 100), 2, 2),
        ArrayTile(Array[Int](100, 200, 100, 100), 2, 2),
        ArrayTile(Array[Int](100, 100, 200, 100), 2, 2),
        ArrayTile(Array[Int](100, 100, 100, 200), 2, 2)
      )

      tiles.foreach(testBilinearSquareCenter(_, 125))
    }

    it("should interpolate correctly when values different in pairs") {
      val tiles = List(
        ArrayTile(Array[Int](200, 100, 200, 100), 2, 2),
        ArrayTile(Array[Int](100, 200, 200, 100), 2, 2),
        ArrayTile(Array[Int](200, 100, 100, 200), 2, 2),
        ArrayTile(Array[Int](100, 200, 100, 200), 2, 2),
        ArrayTile(Array[Int](200, 200, 100, 100), 2, 2),
        ArrayTile(Array[Int](100, 100, 200, 200), 2, 2)
      )

      tiles.foreach(testBilinearSquareCenter(_, 150))
    }

    it("should interpolate correctly with two different values and two same") {
      val tiles = List(
        ArrayTile(Array[Int](200, 100, 300, 100), 2, 2),
        ArrayTile(Array[Int](100, 300, 200, 100), 2, 2),
        ArrayTile(Array[Int](200, 100, 100, 300), 2, 2),
        ArrayTile(Array[Int](100, 300, 100, 200), 2, 2),
        ArrayTile(Array[Int](300, 200, 100, 100), 2, 2),
        ArrayTile(Array[Int](100, 100, 200, 300), 2, 2)
      )

      tiles.foreach(testBilinearSquareCenter(_, 175))
    }

    it("should interpolate correctly with all values different") {
      val tiles = List(
        ArrayTile(Array[Int](200, 0, 300, 100), 2, 2),
        ArrayTile(Array[Int](100, 300, 200, 0), 2, 2),
        ArrayTile(Array[Int](200, 0, 100, 300), 2, 2),
        ArrayTile(Array[Int](100, 300, 0, 200), 2, 2),
        ArrayTile(Array[Int](300, 200, 0, 100), 2, 2),
        ArrayTile(Array[Int](0, 100, 200, 300), 2, 2)
      )

      tiles.foreach(testBilinearSquareCenter(_, 150))
    }

  }

  describe("interpolates correctly at various points in bounding rectangle") {

    it("should interpolate correctly when all values are same and points are varying") {
      val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
      val res = 100

      for (x <- 1 to 99; y <- 1 to 99) {
        val (xr, yr) = (x.toDouble / 100, y.toDouble / 100)
        testInterpolationDouble(xr, yr, res, tile)
      }
    }

    it("should interpolate correctly when varying values and points") {

      // 1 contrib = 1 * 0.75 * 0.75 = 0.5625
      // 2 contrib = 2 * 0.75 * 0.25 = 0.375
      // 3 contrib = 3 * 0.75 * 0.25 = 0.5625
      // 4 contrib = 4 * 0.25 * 0.25 = 0.25
      // accum divisor = 1
      // res = 1.75
      testInterpolationDouble(0.375, 0.625, 1.75)

      // 1 contrib: 1 * 0.49 * 1 = 0.49
      // 2 contrib: 2 * 0.51 * 1 = 1.02
      // 3 contrib: 0
      // 4 contrib: 0
      // accum divisor = 1
      // res = 1.51
      testInterpolationDouble(0.51, 1, 1.52)

      // 1 contrib: 0
      // 2 contrib: 0
      // 3 contrib: 3 * 0.51 * 1 = 1.53
      // 4 contrib: 4 * 0.49 * 1 = 1.96
      // accum divisor = 1
      // res = 3.49
      testInterpolationDouble(0.49, 0, 3.48)
    }

  }

  describe("interpolates correctly at various points at border of rectangle") {

    it("should interpolate correctly when all values are same and points are varying") {
      val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
      val res = 100
      val extent = Extent(0, 0, 1, 1)

      for (y <- 1 to 99) {
        val (xr, yr) = (0, y.toDouble / 100)
        testInterpolationDouble(xr, yr, res, tile)
      }

      for (y <- 1 to 99) {
        val (xr, yr) = (1, y.toDouble / 100)
        testInterpolationDouble(xr, yr, res, tile)
      }

      for (x <- 1 to 99) {
        val (xr, yr) = (x.toDouble / 100, 0)
        testInterpolationDouble(xr, yr, res, tile)
      }

      for (x <- 1 to 99) {
        val (xr, yr) = (x.toDouble / 100, 1)
        testInterpolationDouble(xr, yr, res, tile)
      }
    }

  }

  describe("resolving top left coordinates and ratios should work correctly") {

    def testResolvingCoordsAndRatios(
      x: Double,
      y: Double,
      extent: Extent = Extent(0, 0, 1, 1)) = {
      val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
      val bi = new BilinearInterpolation(tile, extent)
      bi.resolveTopLeftCoordsAndRatios(x, y)
    }

    it("should resolve correct coordinates and ratios for x = 50% and y = 50%") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0.5, 0.5)

      xc should be (0)
      yc should be (0)
      xr should be (0.5)
      yr should be (0.5)
    }

    // Since half of the extent cellwidth/cellheight is 0.25,
    // then 25% of the actual area is 0.125.
    it("should resolve correct coordinates and ratios for x = 25% and y = 25%") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0.375, 0.625)

      xc should be (0)
      yc should be (0)
      xr should be (0.25)
      yr should be (0.25)
    }

    it("should resolve correct coordinates and ratios for x = 25% and y = 75%") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0.375, 0.375)

      xc should be (0)
      yc should be (0)
      xr should be (0.25)
      yr should be (0.75)
    }

    it("should resolve correct coordinates and ratios for x = 75% and y = 75%") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0.625, 0.375)

      xc should be (0)
      yc should be (0)
      xr should be (0.75)
      yr should be (0.75)
    }

    it("should resolve correct coordinates and ratios for x = 75% and y = 25%") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0.625, 0.625)

      xc should be (0)
      yc should be (0)
      xr should be (0.75)
      yr should be (0.25)
    }

    it("should resolve correct coordinates and ratios when out of bounds top") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0.5, 1.0)

      xc should be (0)
      yc should be (-1)
      xr should be (0.5)
      yr should be (1)
    }

    it("should resolve correct coordinates and ratios when out of bounds bottom") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0.5, 0.0)

      xc should be (0)
      yc should be (1)
      xr should be (0.5)
      yr should be (0)
    }

    it("should resolve correct coordinates and ratios when out of bounds left") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0, 0.5)

      xc should be (-1)
      yc should be (0)
      xr should be (1)
      yr should be (0.5)
    }

    it("should resolve correct coordinates and ratios when out of bounds right") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(1, 0.5)

      xc should be (1)
      yc should be (0)
      xr should be (0)
      yr should be (0.5)
    }

    it("should resolve correct coordinates and ratios when out of bounds top left") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0, 1)

      xc should be (-1)
      yc should be (-1)
      xr should be (1)
      yr should be (1)
    }

    it("should resolve correct coordinates and ratios when out of bounds top right") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(1, 1)

      xc should be (1)
      yc should be (-1)
      xr should be (0)
      yr should be (1)
    }

    it("should resolve correct coordinates and ratios when out of bounds bottom left") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(0, 0)

      xc should be (-1)
      yc should be (1)
      xr should be (1)
      yr should be (0)
    }

    it("should resolve correct coordinates and ratios when out of bounds bottom right") {
      val (xc, yc, xr, yr) = testResolvingCoordsAndRatios(1, 0)

      xc should be (1)
      yc should be (1)
      xr should be (0)
      yr should be (0)
    }

  }

  describe("interpolation should work correctly") {

    it("should interpolate correctly at top left corner") {
      testInterpolationInt(0, 1, 1)
      testInterpolationDouble(0, 1, 1)
    }

    it("should interpolate correctly at top right corner") {
      testInterpolationInt(1, 1, 2)
      testInterpolationDouble(1, 1, 2)
    }

    it("should interpolate correctly at bottom left corner") {
      testInterpolationInt(0, 0, 3)
      testInterpolationDouble(0, 0, 3)
    }

    it("should interpolate correctly at bottom right corner") {
      testInterpolationInt(1, 0, 4)
      testInterpolationDouble(1, 0, 4)
    }

    it("should interpolate correctly at top center") {
      testInterpolationInt(0.5, 1, 2)
      testInterpolationDouble(0.5, 1, 1.5)
    }

    it("should interpolate correctly at bottom center") {
      testInterpolationInt(0.5, 0, 4)
      testInterpolationDouble(0.5, 0, 3.5)
    }

    it("should interpolate correctly at left center") {
      testInterpolationInt(0, 0.5, 2)
      testInterpolationDouble(0, 0.5, 2)
    }

    it("should interpolate correctly at right center") {
      testInterpolationInt(1, 0.5, 3)
      testInterpolationDouble(1, 0.5, 3)
    }

    it("should interpolate correctly at center") {
      testInterpolationInt(0.5, 0.5, 3)
      testInterpolationDouble(0.5, 0.5, 2.5)
    }

    it("should interpolate correctly at x = 0.25, y = 0.25") {
      testInterpolationInt(0.375, 0.625, 2)
      testInterpolationDouble(0.375, 0.625, 1.75)
    }

    it("should interpolate correctly at x = 0.25, y = 0.75") {
      testInterpolationInt(0.375, 0.375, 3)
      testInterpolationDouble(0.375, 0.375, 2.75)
    }

    it("should interpolate correctly at x = 0.75, y = 0.25") {
      testInterpolationInt(0.625, 0.625, 2)
      testInterpolationDouble(0.625, 0.625, 2.25)
    }

    it("should interpolate correctly at x = 0.75, y = 0.75") {
      testInterpolationInt(0.625, 0.375, 3)
      testInterpolationDouble(0.625, 0.375, 3.25)
    }

  }

}
