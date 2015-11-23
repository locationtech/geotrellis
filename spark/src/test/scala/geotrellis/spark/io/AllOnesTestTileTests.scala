package geotrellis.spark.io

import geotrellis.raster.{GridBounds, Tile}
import geotrellis.spark.{OnlyIfCanRunSpark, SpatialKey}
import geotrellis.vector.Extent

trait AllOnesTestTileTests { self: PersistenceSpec[SpatialKey, Tile] with OnlyIfCanRunSpark =>

  val bounds1 = GridBounds(1,1,3,3)
  val bounds2 = GridBounds(4,5,6,6)

  if (canRunSpark) {

    it("filters past layout bounds") {
      query.where(Intersects(GridBounds(6, 2, 7, 3))).toRDD.keys.collect() should
        contain theSameElementsAs Array(SpatialKey(6, 3), SpatialKey(6, 2))
    }

    it("query inside layer bounds") {
      val actual = query.where(Intersects(bounds1)).toRDD.keys.collect()
      val expected = for ((x, y) <- bounds1.coords) yield SpatialKey(x, y)

      if (expected.diff(actual).nonEmpty)
        info(s"missing: ${(expected diff actual).toList}")
      if (actual.diff(expected).nonEmpty)
        info(s"unwanted: ${(actual diff expected).toList}")

      actual should contain theSameElementsAs expected
    }

    it("query outside of layer bounds") {
      query.where(Intersects(GridBounds(10, 10, 15, 15))).toRDD.collect() should be(empty)
    }

    it("disjoint query on space") {
      val actual = query.where(Intersects(bounds1) or Intersects(bounds2)).toRDD.keys.collect()
      val expected = for ((x, y) <- bounds1.coords ++ bounds2.coords) yield SpatialKey(x, y)

      if (expected.diff(actual).nonEmpty)
        info(s"missing: ${(expected diff actual).toList}")
      if (actual.diff(expected).nonEmpty)
        info(s"unwanted: ${(actual diff expected).toList}")

      actual should contain theSameElementsAs expected
    }

    it("should filter by extent") {
      val extent = Extent(-10, -10, 10, 10) // this should intersect the four central tiles in 8x8 layout
      query.where(Intersects(extent)).toRDD.keys.collect() should
        contain theSameElementsAs {
        for ((col, row) <- GridBounds(3, 3, 4, 4).coords) yield SpatialKey(col, row)
      }
    }
  }
}
