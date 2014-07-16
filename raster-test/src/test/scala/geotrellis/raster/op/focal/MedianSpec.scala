package geotrellis.raster.op.focal

import org.scalatest._
import geotrellis.testkit._

class MedianSpec extends FunSpec with Matchers with TileBuilders with TestEngine {
  describe("Tile focalMedian") {

    it("square median") {
      val input = Array[Int](
        nd,7,1,     1,3,5,      9,8,2,
        9,1,1,      2,2,2,      4,3,5,

        3,8,1,     3, 3,3,      1,2,2,
        2,4,7,     1,nd,1,      8,4,3)

      val expected = Array[Int](
        7, 1, 1,    1, 2, 3,    4, 4, 4,
        7, 2, 1,    2, 3, 3,    3, 3, 2,

        3, 3, 2,    2, 2, 2,    3, 3, 3,
        3, 3, 3,    3, 3, 3,    2, 2, 2)

      val inputTile = createTile(input, 9, 4)
      val expectedTile = createTile(expected, 9, 4)
      assertEqual(inputTile.focalMedian(Square(1)), expectedTile, threshold = 0.001)
    }
  }
}
