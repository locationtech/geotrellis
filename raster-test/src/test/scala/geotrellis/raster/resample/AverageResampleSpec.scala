package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class AverageResampleSpec extends FunSpec with Matchers {
/*
  describe("it should resample to nodata when only nodata in tile") {

    it("should, for an integer tile, compute nodata as average") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(NODATA), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val resamp = new AverageResample(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        resamp.resample(i / 10.0, j / 10.0) should be (NODATA)
    }

    it("should, for a double tile, compute nodata as average") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(Double.NaN), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val resamp = new AverageResample(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        assert(resamp.resampleDouble(i / 10.0, j / 10.0).isNaN)
    }

  }
*/
  describe("it should correctly resample to the average") {

    it("should gather all and only the relevant coordinates") {
      val cols = 4
      val rows = 2
      val tile = ArrayTile(
        Array(1, 3, 6, 8,
              2, 4, 6, 8),
        cols, rows)

      val extent = Extent(0, 0, cols, rows)
      val cellsize = CellSize(extent, 2, 1)
      val resamp = new AverageResample(tile, extent, cellsize) {}
      resamp.contributions(0.0, 0.0) should be (Vector((0,0), (0,1), (1,0), (1,1)))
    }

    it("should resample to averages for a region") {
      val cols = 4
      val rows = 2
      val tile = ArrayTile(
        Array(1, 3, 6, 8,
              2, 4, 6, 8),
        cols, rows)

      val extent = Extent(0, 0, cols, rows)
      val cellsize = CellSize(extent, cols, rows)
      val resamp = new AverageResample(tile, extent, cellsize) {}
      println(resamp.contributions(0, 0.5), resamp.contributions(2, 0.5))
      println(resamp.resample(0, 0.5), resamp.resample(2, 0.5))
      println(tile.resample(extent, Extent(0, 0, 4, 2), Average).asciiDraw)
      resamp.contributions(0.0, 0.0) should be (Vector((0,0), (0,1), (1,0), (1,1)))
    }

    it("should for a integer tile with all values same compute the correct average") {
      val cols = 4
      val rows = 1
      val tile = ArrayTile(
        Array(1, 3, 6, 8),
        cols, rows)

      val extent = Extent(0, 0, cols, rows)
      val cellsize = CellSize(extent, 2, 1)
      val resamp = new AverageResample(tile, extent, cellsize)
      resamp.resample(0.0, 0.0) should be (2)
    }
/*
    it("should for a double tile with all values same compute the correct average") {
      val cols = 100
      val rows = 100
      val avg = Double.MaxValue / 2

      val tile = ArrayTile(Array.fill(cols * rows)(avg), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val resamp = new AverageResample(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        resamp.resampleDouble(i / 10.0, j / 10.0) should be (avg)
    }

    it("should for a integer tile with different values same compute the correct average") {
      val cols = 100
      val rows = 100

      val n = cols * rows
      val arr = List.range(1, n + 1).toArray

      val avg = ((n + 1) / 2.0).round.toInt

      val tile = ArrayTile(arr, cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val resamp = new AverageResample(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        resamp.resample(i / 10.0, j / 10.0) should be (avg)
    }

    it("should for a double tile with different values same compute the correct average") {
      val cols = 100
      val rows = 100

      val n = cols * rows
      val arr = List.range(1, n + 1).toArray

      val avg = (n + 1) / 2.0

      val tile = ArrayTile(arr, cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val resamp = new AverageResample(tile, extent)
      for (i <- 0 until cols * 10; j <- 0 until rows * 10)
        resamp.resampleDouble(i / 10.0, j / 10.0) should be (avg)
    }

*/
  }
}
