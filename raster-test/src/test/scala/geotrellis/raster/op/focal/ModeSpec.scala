package geotrellis.raster.op.focal

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

      val expected = Array[Int](
        nd, 1, 1,    1, 2, 2,   nd,nd,nd,
        nd, 1, 1,    1, 3, 3,   nd, 2, 2,

        nd, 1, 1,    1,nd,nd,   nd,nd,nd,
        nd,nd, 1,   nd, 3,nd,    1, 2, 2)

      val inputTile = createTile(input, 9, 4)
      val expectedTile = createTile(expected, 9, 4)
      val actualTile = inputTile.focalMode(Square(1))

      assertEqual(actualTile, expectedTile, threshold = 0.001)
    }
  }
}
