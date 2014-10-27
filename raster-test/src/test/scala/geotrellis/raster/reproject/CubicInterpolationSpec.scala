package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

/*
 * Since cubic interpolation inherits from bilinear interpolation
 * there is no need to actually test that the implementation resolves
 * the correct points given the extent coordinates, since that is tested
 * in the bilinear interpolation testing specification.
 *
 * Instead, this specification tests actual cubic interpolation functionality,
 * that the implementation correctly falls back to bilinear when 16 points
 * surrounding the map coordinate point can't be resolved, and that the
 * interpolation as a whole works.
 *
 */
class CubicInterpolationSpec extends FunSpec with Matchers {

  val Epsilon = 1e-9

  def testInterpolationInt(
    x: Double,
    y: Double,
    res: Double,
    tile: Tile = ArrayTile(List.range(1, 17).toArray, 4, 4),
    extent: Extent = Extent(0, 0, 1, 1)) = {
    val ci = new CubicInterpolation(tile, extent)
    ci.interpolate(x, y) should be (res)
  }

  def testInterpolationDouble(
    x: Double,
    y: Double,
    res: Double,
    tile: Tile = ArrayTile(List.range(1, 17).toArray, 4, 4),
    extent: Extent = Extent(0, 0, 1, 1)) = {
    val ci = new CubicInterpolation(tile, extent)
    ci.interpolateDouble(x, y) should be (res +- Epsilon)
  }

  def testCubicSquareCenter(tile: Tile, result: Double) =
    testInterpolationDouble(0.5, 0.5, result, tile)

  describe("interpolates correctly at square center") {

    it("should interpolate correctly when all values same") {
      val tile = ArrayTile(Array.fill[Int](16)(100), 4, 4)
      testCubicSquareCenter(tile, 100)
    }

    it("should interpolate correctly with one different value and three same") {
      val tile = ArrayTile(
        Array[Int](
          200, 100, 100, 100,
          100, 200, 100, 100,
          100, 100, 200, 100,
          100, 100, 100, 200
        ), 4, 4
      )

      testCubicSquareCenter(tile, 125)
    }

    it("should interpolate correctly when values different in pairs") {
      val tile = ArrayTile(
        Array[Int](
          200, 200, 100, 100,
          100, 200, 100, 200,
          200, 100, 200, 100,
          100, 200, 100, 200
        ), 4, 4
      )

      testCubicSquareCenter(tile, 125)
    }

    it("should interpolate correctly with two different values and two same") {
      val tile = ArrayTile(
        Array[Int](
          200, 200, 100, 300,
          100, 200, 300, 200,
          200, 300, 200, 100,
          100, 200, 300, 200
        ), 4, 4
      )

      testCubicSquareCenter(tile, 125)
    }

    it("should interpolate correctly with all values different") {
      val tile = ArrayTile(
        Array[Int](
          400, 200, 100, 300,
          100, 400, 300, 200,
          200, 300, 400, 100,
          100, 200, 300, 400
        ), 4, 4
      )

      testCubicSquareCenter(tile, 125)
    }

  }

  describe("interpolates correctly at various points in bounding rectangle") {

    it("should interpolate correctly when all values are same and points are varying") {
      val tile = ArrayTile(Array.fill[Int](16)(100), 4, 4)
      val res = 100

      for (x <- 1 to 99; y <- 1 to 99) {
        val (xr, yr) = (x.toDouble / 100, y.toDouble / 100)
        testInterpolationDouble(xr, yr, res, tile)
      }
    }

    it("should interpolate correctly when varying values and points") {
      val t1 = ArrayTile(List.range(1, 17).toArray, 4, 4)

      // 1 contrib = 1 * 0.75 * 0.75 = 0.5625
      // 2 contrib = 2 * 0.75 * 0.25 = 0.375
      // 3 contrib = 3 * 0.75 * 0.25 = 0.5625
      // 4 contrib = 4 * 0.25 * 0.25 = 0.25
      // accum divisor = 1
      // res = 1.75
      testInterpolationDouble(0.25, 0.25, 1.75, t1)

      // 1 contrib: 1 * 0.49 * 1 = 0.49
      // 2 contrib: 2 * 0.51 * 1 = 1.02
      // 3 contrib: 0
      // 4 contrib: 0
      // accum divisor = 1
      // res = 1.51
      testInterpolationDouble(0.51, 0, 1.51, t1)

      // 1 contrib: 0
      // 2 contrib: 0
      // 3 contrib: 3 * 0.51 * 1 = 1.53
      // 4 contrib: 4 * 0.49 * 1 = 1.96
      // accum divisor = 1
      // res = 3.49
      testInterpolationDouble(0.49, 1, 3.49, t1)
    }

    it("should return NODATA when point is outside extent") {
      val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
      val extent = Extent(1, 1, 2, 2)
      val ci = new CubicInterpolation(tile, extent)

      ci.interpolate(0.99, 1.01) should be (NODATA)
      ci.interpolate(1.01, 0.99) should be (NODATA)
      ci.interpolate(2.01, 1.01) should be (NODATA)
      ci.interpolate(1.01, 2.01) should be (NODATA)
    }

  }

}
