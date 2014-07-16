package geotrellis.raster.op.focal

import geotrellis.raster.TileLayout
import org.scalatest._
import geotrellis.testkit._

class ModeSpec extends FunSpec with Matchers with TileBuilders with TestEngine {
  describe("Tile focalMode") {

    it("square mode") {
      val input = Array[Int](
        nd,7,1,     1, 3,5,      9,8,2,
         9,1,1,     2, 2,2,      4,3,5,

         3,8,1,     3, 3,3,      1,2,2,
         2,4,7,     1,nd,1,      8,4,3)

      /*
      note that the order of the cell evaluation is different for a single tile
      vs a RasterSource with multiple tiles. So if there is no mode in the focus
      the results will likely be different between those two cases
      */
      val expected = Array[Int](
        7, 1, 1,    1, 2, 2,    2, 9, 8,
        7, 1, 1,    1, 3, 3,    2, 2, 2,

        9, 1, 1,    1, 2, 1,    2, 2, 2,
        3, 3, 1,    1, 3, 1,    1, 2, 2)

      val inputTile = createCompositeTile(input, TileLayout(3,2,3,2))
      val expectedTile = createTile(expected, 9, 4)
      val actualTile = inputTile.focalMode(Square(1))

      assertEqual(actualTile, expectedTile, threshold = 0.001)
    }
  }
}
