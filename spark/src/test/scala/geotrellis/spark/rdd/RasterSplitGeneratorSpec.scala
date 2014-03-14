package geotrellis.spark.rdd

import geotrellis.spark.tiling.TileExtent

import org.scalatest.FunSpec

import org.scalatest.matchers.ShouldMatchers

class RasterSplitGeneratorSpec extends FunSpec with ShouldMatchers {

  describe("RasterSplitGenerator") {
    it("should yield an increment of -1 (no splits) if tilesPerBlock >= tileCount") {
      RasterSplitGenerator.computeIncrement(TileExtent(0, 0, 1, 1), 1, 4) should be(-1)
      RasterSplitGenerator.computeIncrement(TileExtent(0, 0, 1, 1), 1, 5) should be(-1)
    }
    it("should yield an increment of 1 (row per split) if tileExtent.width > tilesPerBlock") {
      RasterSplitGenerator.computeIncrement(TileExtent(0, 0, 1, 1), 1, 1) should be(1)
    }
    it("should yield an increment of > 1 (row per split) if tileCount > tilesPerBlock >= tileExtent.width") {
      RasterSplitGenerator.computeIncrement(TileExtent(0, 0, 2, 2), 1, 6) should be(2)
    }
  }
}
