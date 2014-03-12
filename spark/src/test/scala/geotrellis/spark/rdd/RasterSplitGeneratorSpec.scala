package geotrellis.spark.rdd

import geotrellis.spark.tiling.TileExtent

import org.scalatest.FunSpec

import org.scalatest.matchers.ShouldMatchers

class RasterSplitGeneratorSpec extends FunSpec with ShouldMatchers {

  describe("RasterSplitGenerator") {
    it("should die when tileExtent.width > tilesPerBlock") {
      evaluating {
        // tileExtent.width = 3 > tilesPerBlock = 2
        val gen = RasterSplitGenerator(TileExtent(0, 0, 2, 1), 0, 1, 2)
      } should produce[java.lang.AssertionError];
    }
  }
}
