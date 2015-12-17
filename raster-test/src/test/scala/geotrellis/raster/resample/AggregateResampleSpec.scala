package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class AggregateResampleSpec extends FunSpec with Matchers {

    val B = 5 // value returned when resampling

  class MockAggregateResample(tile: Tile, extent: Extent, targetCS: CellSize) extends AggregateResample(tile, extent, targetCS) {
    protected def resampleValid(x: Double, y: Double): Int = B
    protected def resampleDoubleValid(x: Double, y: Double): Double = B
  }

  describe("aggregate resampling requires assembling a list of contributing cells") {

    it("should assemble contributing cell indexes correctly for a given cellsize") {
      /* Given an initial tile of 2columns/2rows and resizing it to 1column/2rows
       * we should expect that the contributing cells at (0, 0) include only (0, 0)
       * we should expect that the contributing cells at (1.5, 1.25) include 0,0 and 1,0
       * we should expect that the contributing cells at (1.5, 1.75) include 0,1 and 1,1
       */
      val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
      val extent = Extent(0, 0, 2, 2)
      val cellSize = CellSize(extent, 1, 2)

      val resamp = new MockAggregateResample(tile, extent, cellSize)
      resamp.contributions(0, 0) should be (Vector((0, 1), (1, 1)))
      resamp.contributions(1.5, 1.25) should be (Vector((1, 0), (1, 1)))
      resamp.contributions(1.5, 1.75) should be (Vector((1, 0), (1, 1)))
    }

    it("should correctly return the xIndices for a given x coordinate") {
      val tile = IntArrayTile.fill(0, 10, 1)
      val extent = Extent(0, 0, 10, 10)
      val cellSize = CellSize(extent, 1, 1)
      val resamp = new MockAggregateResample(tile, extent, cellSize)
      resamp.xIndices(5) should be ((0, 9))
    }

    it("should correctly return the yIndices for a given y coordinate") {
      val tile = IntArrayTile.fill(0, 1, 10)
      val extent = Extent(0, 0, 10, 10)
      val cellSize = CellSize(extent, 1, 1)
      val resamp = new MockAggregateResample(tile, extent, cellSize)
      resamp.yIndices(5) should be ((0, 9))
    }

    it("should correctly return the xIndices for complex cases") {
      val tile1 = IntArrayTile.fill(0, 20, 1)
      val extent1 = Extent(0, 0, 10, 10)
      val cellSize1 = CellSize(extent1, 10, 1)
      val resamp1 = new MockAggregateResample(tile1, extent1, cellSize1)
      resamp1.xIndices(5) should be ((9, 11))
      resamp1.yIndices(5) should be ((0, 0))

      val tile2 = IntArrayTile.fill(0, 70, 70)
      val extent2 = Extent(0, 0, 10, 10)
      val cellSize2 = CellSize(extent2, 10, 10)
      val resamp2 = new MockAggregateResample(tile2, extent2, cellSize2)
      resamp2.xIndices(5) should be ((32, 38))
      resamp2.yIndices(5) should be ((32, 38))
    }
  }
}
