package geotrellis.raster.op.focal

import org.scalatest._
import geotrellis.testkit._
import geotrellis.raster._

class MinSpec extends FunSpec with Matchers with TileBuilders with TestEngine {
  describe("Tile focalMin") {
    val tile: Tile = createTile((0 until 16).toArray)

    it("square min r=1") {
      val expected = Array(
        0, 0, 1, 2,
        0, 0, 1, 2,
        4, 4, 5, 6,
        8, 8, 9, 10)

      assertEqual(tile.focalMin(Square(1)), createTile(expected))
    }

    it("square min r=2") {
      val expected = Array(
        0, 0, 0, 1,
        0, 0, 0, 1,
        0, 0, 0, 1,
        4, 4, 4, 5)

      assertEqual(tile.focalMin(Square(2)), createTile(expected))
    }

    it("circle min r=2") {
      val expected = Array(
        0, 0, 0, 1,
        0, 0, 1, 2,
        0, 1, 2, 3,
        4, 5, 6, 7)

      assertEqual(tile.focalMin(Circle(2)), createTile(expected))
    }

    it("square min r=1 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,3,5,      9,8,2,
        9,1,1,      2,2,2,      4,3,5,

        3,8,1,      3,3,3,      1,2,2,
        2,4,7,     1,nd,1,      8,4,3)


      val expected = Array(
        1, 1, 1,    1, 1, 2,    2, 2, 2,
        1, 1, 1,    1, 1, 1,    1, 1, 2,

        1, 1, 1,    1, 1, 1,    1, 1, 2,
        2, 1, 1,    1, 1, 1,    1, 1, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))

      assertEqual(input.focalMin(Square(1)), createTile(expected, 9, 4))
    }

    it("square min for composite tile with double"){
      val tile = Array(
        NaN,7.1,1.2,      1.4,3.9,5.1,      9.9,8.1,2.2,
        9.4,1.1,1.5,      2.5,2.2,2.9,      4.0,3.3,5.1,

        3.4,8.2,1.9,      3.8,3.1,3.0,      1.3,2.1,2.5,
        2.5,4.9,7.1,      1.4,NaN,1.1,      8.0,4.8,3.0
      )

      val expected = Array(
        1.1, 1.1, 1.1,    1.2, 1.4, 2.2,    2.9, 2.2, 2.2,
        1.1, 1.1, 1.1,    1.2, 1.4, 1.3,    1.3, 1.3, 2.1,

        1.1, 1.1, 1.1,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1,
        2.5, 1.9, 1.4,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))

      assertEqual(input.focalMin(Square(1)), createTile(expected, 9, 4))
    }

    it("cicle min for composite tile with double"){
      val tile =Array(
        nd,7,4,     5, 4,2,      9,nd,nd,
        9,6,2,     2, 2,2,      5,3,nd,

        3,8,4,     3, 3,3,      3,9,2,
        2,9,7,     4,nd,9,      8,8,4
      )

      val expected = Array(
        7, 4, 2,    2, 2, 2,    2, 3, nd,
        3, 2, 2,    2, 2, 2,    2, 3, 2,

        2, 3, 2,    2, 2, 2,    3, 2, 2,
        2, 2, 4,    3, 3, 3,    3, 4, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))

      assertEqual(input.focalMin(Circle(1)), createTile(expected, 9, 4))
    }

  }


}
