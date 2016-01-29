package geotrellis.raster.stitch

import geotrellis.raster._
import geotrellis.testkit._

import org.scalatest._

class StitcherSpec extends FunSpec with Matchers 
                                   with TileBuilders {
  describe("Stitcher[Tile]") {
    it("should stitch a buffered tile with top missing") {
      val tiles = 
        Seq(
          (IntArrayTile(Array(1, 1), 1, 2), (0, 0)), // Left
          (IntArrayTile(Array(3, 3, 3), 3, 1), (1, 2)), // Bottom
          (IntArrayTile(Array(1, 3, 5, 2, 2, 2), 3, 2), (1, 0)), // Center
          (IntArrayTile(Array(1), 1, 1), (0, 2)), //Bottom left
          (IntArrayTile(Array(1), 1, 1), (4, 2)), // Bottom Right
          (IntArrayTile(Array(9, 4), 1, 2), (4, 0)) // Right
        )
      val actual = implicitly[Stitcher[Tile]].stitch(tiles, 5, 3)

      val expected = 
        ArrayTile(Array(
          1,   1, 3, 5,   9,
          1,   2, 2, 2,   4,
          1,   3, 3, 3,   1
        ), 5, 3)

      actual.toArray should be (expected.toArray)
    }
  }
}
