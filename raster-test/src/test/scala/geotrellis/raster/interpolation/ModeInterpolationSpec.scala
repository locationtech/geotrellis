package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class ModeInterpolationSpec extends FunSpec with Matchers {

  describe("it should interpolate to nodata when only nodata in tile") {

    it("should for a integer tile compute nodata as most common value") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(NODATA), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new ModeInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolate(i / 10.0, j / 10.0) should be (NODATA)
    }

    it("should for a double tile compute nodata as most common value") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(Double.NaN), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new ModeInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        assert(interp.interpolateDouble(i / 10.0, j / 10.0).isNaN)
    }

  }

  describe("it should correctly interpolate to the most common value") {

    it("should for a int tile with all values same compute the most common value") {
      val cols = 100
      val rows = 100
      val mcv = Int.MaxValue / 2

      val tile = ArrayTile(Array.fill(cols * rows)(mcv), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new ModeInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolate(i / 10.0, j / 10.0) should be (mcv)
    }

    it("should for a double tile with all values same compute the most common value") {
      val cols = 100
      val rows = 100
      val mcv = Double.MaxValue / 2

      val tile = ArrayTile(Array.fill(cols * rows)(mcv), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new ModeInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolateDouble(i / 10.0, j / 10.0) should be (mcv)
    }

    it("should for a int tile with different values same compute the most common value") {
      val cols = 100
      val rows = 100

      val mcv = 1337
      val arr = Array.ofDim[Double](cols * rows)
      for (i <- 0 until cols * rows) {
        arr(i) = if (i % 2 == 0) mcv else i
      }

      val tile = ArrayTile(arr, cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new ModeInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolate(i / 10.0, j / 10.0) should be (mcv)
    }

    it("should for a double tile with different values same compute the most common value") {
      val cols = 100
      val rows = 100

      val mcv = 1337.13
      val arr = Array.ofDim[Double](cols * rows)
      for (i <- 0 until cols * rows) {
        arr(i) = if (i % 2 == 0) mcv else i / 1.1337
      }

      val tile = ArrayTile(arr, cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new ModeInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolateDouble(i / 10.0, j / 10.0) should be (mcv)
    }

  }

}
