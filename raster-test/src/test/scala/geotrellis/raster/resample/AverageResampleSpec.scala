package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class AverageResampleSpec extends FunSpec with Matchers {

  describe("it should resample to nodata when only nodata in tile") {

    it("should, for an integer tile, compute nodata as average") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(NODATA), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val cellsize = CellSize(extent, cols, rows)
      val resamp = new AverageResample(tile, extent, cellsize)
      for (i <- 0 until cols; j <- 0 until rows)
        resamp.resample(i, j) should be (NODATA)
    }

    it("should, for a double tile, compute nodata as average") {
      val cols = 100
      val rows = 100

      val tile = ArrayTile(Array.fill(cols * rows)(Double.NaN), cols, rows)
      val extent = Extent(0, 0, cols, rows)
      val cellsize = CellSize(extent, cols, rows)
      val resamp = new AverageResample(tile, extent, cellsize)
      for (i <- 0 until cols; j <- 0 until rows)
        assert(resamp.resampleDouble(i, j).isNaN)
    }

  }

  describe("it should correctly resample to the average of a region determined by cellsize") {

    it("should gather all and only the relevant coordinates and correctly resample to their averages for int based tiles") {
      val cols = 4
      val rows = 2
      val tile = IntArrayTile(
        Array(1, 3, 6, 8,
              2, 4, 6, 8),
        cols, rows)

      val extent = Extent(0, 0, cols, rows)
      val cellsize = CellSize(extent, 2, 1)
      val resamp = new AverageResample(tile, extent, cellsize)
      resamp.xIndices(0.0) should be ((0, 1))
      resamp.yIndices(0.0) should be ((1, 1))
      resamp.contributions(0.0, 0.0) should be (Vector((0,1), (1,1)))
      resamp.resample(0.0, 0.0) should be (3)  // 10/4 is 2 when rounded to int

      resamp.contributions(3.0, 1.0) should be (Vector((2,0), (2,1), (3,0), (3,1)))
      resamp.resample(3.0, 1.0) should be (7)  // 28/4 is 7 when rounded to int

      resamp.contributions(2.0, 1.0) should be (Vector((1,0), (1,1), (2,0), (2,1), (3,0), (3,1)))
      resamp.resample(2.0, 1.0) should be (5)  // 35/6 is 2 when rounded to int
    }

    it("should gather all and only the relevant coordinates and correctly resample to their averages for float based tiles") {
      val cols = 4
      val rows = 2
      val tile = DoubleArrayTile(
        Array(1.0, 3.0, 6.0, 8.0,
              2.0, 4.0, 6.0, 8.0),
        cols, rows)

      val extent = Extent(0, 0, cols, rows)
      val cellsize = CellSize(extent, 2, 1)
      val resamp = new AverageResample(tile, extent, cellsize) {}
      resamp.xIndices(0.0) should be ((0, 1))
      resamp.yIndices(1.0) should be ((0, 1))
      resamp.contributions(0.0, 1.0) should be (Vector((0, 0), (0,1), (1, 0), (1,1)))
      resamp.resampleDouble(0.0, 1.0) should be (2.5)  // 10/4 is 2.5

      resamp.contributions(3.0, 1.0) should be (Vector((2,0), (2,1), (3,0), (3,1)))
      resamp.resampleDouble(3.0, 1.0) should be (7)  // 28/4 is 7

      resamp.contributions(2.0, 1.0) should be (Vector((1,0), (1,1), (2,0), (2,1), (3,0), (3,1)))
      resamp.resampleDouble(2.0, 1.0) should be (5.83 +- 0.01)  // 35/6 is roughly 5.83
    }
  }
}
