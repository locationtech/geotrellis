package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class ModeResampleSpec extends FunSpec with Matchers {

  describe("it should resample to nodata when only nodata in tile") {

    it("should for an integer tile compute nodata as most common value") {
      val tile = ArrayTile(Array(1, 2, 3, 4, 5, 6, 7, 19, 19), 3, 3)
      val extent = Extent(0, 0, 10, 10)
      val cellsize = CellSize(extent, 10, 10)
      val resamp = new ModeResample(tile, extent, cellsize)
      tile.resample(extent, 1, 1, Mode).get(0, 0) should be (19)
    }

    it("should for a double tile compute nodata as most common value") {
      val tile = DoubleArrayTile(Array(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.19, 0.19), 3, 3)
      val extent = Extent(0, 0, 3, 3)
      val cellsize = CellSize(extent, 3, 3)
      val resamp = new ModeResample(tile, extent, cellsize)
      tile.resample(extent, 1, 1, Mode).getDouble(0, 0) should be (0.19)
    }
  }
}
