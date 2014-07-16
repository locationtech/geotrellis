package geotrellis.raster.op.focal

import org.scalatest._
import geotrellis.testkit._
import geotrellis.raster._

class MaxSpec extends FunSpec with Matchers with TileBuilders with TestEngine {
  describe("Tile focalMax") {

    it("square max r=1") {
      val tile: Tile = createTile(Array[Int](
        1,1,1,1,
        2,2,2,2,
        3,3,3,3,
        1,1,4,4))

      val expected = Array[Int](
        2,2,2,2,
        3,3,3,3,
        3,4,4,4,
        3,4,4,4)

      tile.focalMax(Square(1)) should be (createTile(expected))
    }

    it("square min r=1 on doubles what the fuck?") {
      val input = createTile(Array(
        1.2,1.3,1.1,1.4,
        2.4,2.1,2.5,2.2,
        3.1,3.5,3.2,3.1,
        1.9,1.1,4.4,4.9))

      val expected = Array[Double](
        2.4,2.5,2.5,2.5,
        3.5,3.5,3.5,3.2,
        3.5,4.4,4.9,4.9,
        3.5,4.4,4.9,4.9)

      assertEqual(input.focalMax(Square(1)), createTile(expected))
    }

    it("square max r=1 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,1,1,      1,1,1,
        9,1,1,      2,2,2,      1,3,1,

        3,8,1,      3,3,3,      1,1,2,
        2,1,7,     1,nd,1,      8,1,1)


      val expected = Array(
        9, 9, 7,    2, 2, 2,    3, 3, 3,
        9, 9, 8,    3, 3, 3,    3, 3, 3,

        9, 9, 8,    7, 3, 8,    8, 8, 3,
        8, 8, 8,    7, 3, 8,    8, 8, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))
      assertEqual(input.focalMax(Square(1)), createTile(expected, 9, 4))
    }

    it("square max r=2 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,1,1,      1,1,1,
        9,1,1,      2,2,2,      1,3,1,

        3,8,1,      3,3,3,      1,1,2,
        2,1,7,     1,nd,1,      8,1,1)


      val expected = Array(
        9, 9, 9,    8, 3, 3,    3, 3, 3,
        9, 9, 9,    8, 8, 8,    8, 8, 8,

        9, 9, 9,    8, 8, 8,    8, 8, 8,
        9, 9, 9,    8, 8, 8,    8, 8, 8)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))
      assertEqual(input.focalMax(Square(2)), createTile(expected, 9, 4))
    }

    it("circle max r=1 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,1,1,      1,1,1,
        9,1,1,      2,2,2,      1,3,1,

        3,8,1,      3,3,3,      1,1,2,
        2,1,7,     1,nd,1,      8,1,1)

      val expected = Array(
        9, 7, 7,    2, 2, 2,    1, 3, 1,
        9, 9, 2,    3, 3, 3,    3, 3, 3,

        9, 8, 8,    3, 3, 3,    8, 3, 2,
        3, 8, 7,    7, 3, 8,    8, 8, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))
      assertEqual(input.focalMax(Circle(1)), createTile(expected, 9, 4))
    }
  }


}
