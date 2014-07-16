package geotrellis.raster.op.focal

import org.scalatest._
import geotrellis.testkit._

class MeanSpec extends FunSpec with Matchers with TileBuilders with TestEngine {
  describe("Tile focalMean") {

    it("square mean") {
      val input = Array[Int](
        nd, 7, 1, 1, 3, 5, 9, 8, 2,
        9, 1, 1, 2, 2, 2, 4, 3, 5,

        3, 8, 1, 3, 3, 3, 1, 2, 2,
        2, 4, 7, 1, nd, 1, 8, 4, 3)

      val expected = Array(
        5.666, 3.8, 2.166, 1.666, 2.5, 4.166, 5.166, 5.166, 4.5,
        5.6, 3.875, 2.777, 1.888, 2.666, 3.555, 4.111, 4.0, 3.666,

        4.5, 4.0, 3.111, 2.5, 2.125, 3.0, 3.111, 3.555, 3.166,
        4.25, 4.166, 4.0, 3.0, 2.2, 3.2, 3.166, 3.333, 2.75)

      val tile = createTile(input, 9, 4)
      assertEqual(tile.focalMean(Square(1)), createTile(expected, 9, 4), threshold = 0.001)
    }

    it("circle mean for double") {
      val input = Array[Double](
        5.666, 3.8, 2.166, 1.666, 2.5, 4.166, 5.166, 5.166, 4.5,
        5.6, 3.875, 2.777, 1.888, 2.666, 3.555, 4.111, 4.0, 3.666,

        4.5, 4.0, 3.111, 2.5, 2.125, 3.0, 3.111, 3.555, 3.166,
        4.25, 4.166, 4.0, 3.0, 2.2, 3.2, 3.166, 3.333, 2.75)

      val expected = Array[Double](
        5.022, 3.876, 2.602, 2.054, 2.749, 3.846, 4.652, 4.708, 4.444,
        4.910, 4.01, 2.763, 2.299, 2.546, 3.499, 3.988, 4.099, 3.833,

        4.587, 3.93, 3.277, 2.524, 2.498, 2.998, 3.388, 3.433, 3.284,
        4.305, 4.104, 3.569, 2.925, 2.631, 2.891, 3.202, 3.201, 3.083)

      val tile = createTile(input, 9, 4)
      val expectedTile = createTile(expected, 9, 4)
      assertEqual(tile.focalMean(Circle(1)), expectedTile, threshold = 0.002)
    }
  }
}
