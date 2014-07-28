package geotrellis.raster.op.focal

import org.scalatest._
import geotrellis.testkit._

class ModeSpec extends FunSpec with Matchers with FocalOpSpec with TestEngine {

  val getModeResult = Function.uncurried((getCursorResult _).curried(
    (r,n) => Mode.calculation(r,n)))

  describe("Tile focalMode") {
    it("should match worked out results") {
      val r = createTile(Array(
        3, 4, 1, 1, 1,
        7, 4, 0, 1, 0,
        3, 3, 7, 7, 1,
        0, 7, 2, 0, 0,
        6, 6, 6, 5, 5
      ))

      val result = r.focalMode(Square(1))

      result.get(0,0) should equal (4)
      result.get(1,0) should equal (4)
      result.get(2,0) should equal (1)
      result.get(3,0) should equal (1)
      result.get(4,0) should equal (1)
      result.get(0,1) should equal (3)
      result.get(1,1) should equal (3)
      result.get(2,1) should equal (1)
      result.get(3,1) should equal (1)
      result.get(4,1) should equal (1)
      result.get(0,2) should equal (nd)
      result.get(1,2) should equal (7)
      result.get(2,2) should equal (7)
      result.get(3,2) should equal (0)
      result.get(4,2) should equal (0)
      result.get(0,3) should equal (nd)
      result.get(1,3) should equal (6)
      result.get(2,3) should equal (7)
      result.get(3,3) should equal (nd)
      result.get(4,3) should equal (nd)
      result.get(0,4) should equal (6)
      result.get(1,4) should equal (6)
      result.get(2,4) should equal (6)
      result.get(3,4) should equal (nd)
      result.get(4,4) should equal (nd)
    }

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
