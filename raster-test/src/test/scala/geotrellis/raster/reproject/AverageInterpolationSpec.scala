package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class AverageInterpolationSpec extends FunSpec with Matchers {

  describe("it should interpolate to nodata when only nodata in tile") {

    it("should for a integer tile compute nodata as average") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(NODATA), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new AverageInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolate(i / 10.0, j / 10.0) should be (NODATA)
    }

    it("should for a double tile compute nodata as average") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(Double.NaN), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new AverageInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        assert(interp.interpolateDouble(i / 10.0, j / 10.0).isNaN)
    }

  }

  describe("it should correctly interpolate to the average") {

    it("should for a integer tile with all values same compute the correct average") {
      val cols = 100
      val rows = 100
      val avg = Int.MaxValue / 2

      val tile = ArrayTile(Array.fill(cols * rows)(avg), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new AverageInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolate(i / 10.0, j / 10.0) should be (avg)
    }

    it("should for a double tile with all values same compute the correct average") {
      val cols = 100
      val rows = 100
      val avg = Double.MaxValue / 2

      val tile = ArrayTile(Array.fill(cols * rows)(avg), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new AverageInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolateDouble(i / 10.0, j / 10.0) should be (avg)
    }

    it("should for a integer tile with different values same compute the correct average") {
      val cols = 100
      val rows = 100

      val n = cols * rows
      val arr = List.range(1, n + 1).toArray

      val avg = ((n + 1) / 2.0).round.toInt

      val tile = ArrayTile(arr, cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new AverageInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolate(i / 10.0, j / 10.0) should be (avg)
    }

    it("should for a double tile with different values same compute the correct average") {
      val cols = 100
      val rows = 100

      val n = cols * rows
      val arr = List.range(1, n + 1).toArray

      val avg = (n + 1) / 2.0

      val tile = ArrayTile(arr, cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val interp = new AverageInterpolation(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        interp.interpolateDouble(i / 10.0, j / 10.0) should be (avg)
    }

  }

}
