package geotrellis.raster

import geotrellis.raster.testkit._
import geotrellis.vector.Extent
import org.scalatest._

class CroppedTileSpec
  extends FunSpec
    with TileBuilders
    with RasterMatchers
    with TestFiles {

  describe("CroppedTileSpec") {
    it("should combine cropped tile") {
      val r = createTile(
        Array[Int](
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 1, 1, 1, 1))

      val sourceExtent = Extent(0, 0, 5, 5)
      val targetExtent = Extent(1, 1, 4, 4)
      val tile = CroppedTile(r, sourceExtent, targetExtent).toArrayTile

      assertEqual(tile.combine(tile)(_ + _), Array[Int](
        4, 4, 4,
        4, 4, 4,
        4, 4, 4))
    }
  }
}
