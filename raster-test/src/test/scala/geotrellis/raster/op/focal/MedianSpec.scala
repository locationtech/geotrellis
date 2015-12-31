package geotrellis.raster.op.focal

import org.scalatest._
import geotrellis.testkit._

class MedianSpec extends FunSpec with Matchers with TileBuilders with RasterMatchers {
  describe("Tile focalMedian") {
    it("should match worked out results") {
      val r = createTile(Array(
        3, 4, 1, 1, 1,
        7, 4, 0, 1, 0,
        3, 3, 7, 7, 1,
        0, 7, 2, 0, 0,
        6, 6, 6, 5, 5))

      val result = r.focalMedian(Square(1))

      def median(s:Int*) = {
        if(s.length % 2 == 0) {
          val middle = (s.length/2) -1
          (s(middle) + s(middle+1)) / 2
        } else {
          s(s.length/2)
        }
      }

      result.get(0,0) should equal (median(2,4,4,7))
      result.get(1,0) should equal (median(0,1,3,4,4,7))
      result.get(2,0) should equal (median(0,1,1,1,4,4))
      result.get(3,0) should equal (median(0,0,1,1,1,1))
      result.get(4,0) should equal (median(0,1,1,1))
      result.get(0,1) should equal (median(3,3,3,4,4,7))
      result.get(1,1) should equal (median(0,1,3,3,3,4,4,7,7))
      result.get(2,1) should equal (median(0,1,1,1,3,4,4,7,7))
      result.get(3,1) should equal (median(0,0,1,1,1,1,1,7,7))
      result.get(4,1) should equal (median(0,1,1,1,1,7))
      result.get(0,2) should equal (median(0,3,3,4,7,7))
      result.get(1,2) should equal (median(0,0,2,3,3,4,7,7,7))
      result.get(2,2) should equal (median(0,0,1,2,3,4,7,7,7))
      result.get(3,2) should equal (median(0,0,0,0,1,1,2,7,7))
      result.get(4,2) should equal (median(0,0,0,1,1,7))
      result.get(0,3) should equal (median(0,3,3,6,6,7))
      result.get(1,3) should equal (median(0,2,3,3,6,6,6,7,7))
      result.get(2,3) should equal (median(0,2,3,5,6,6,7,7,7))
      result.get(3,3) should equal (median(0,0,1,2,5,5,6,7,7))
      result.get(4,3) should equal (median(0,0,1,5,5,7))
      result.get(0,4) should equal (median(0,6,6,7))
      result.get(1,4) should equal (median(0,2,6,6,6,7))
      result.get(2,4) should equal (median(0,2,5,6,6,7))
      result.get(3,4) should equal (median(0,0,2,5,5,6))
      result.get(4,4) should equal (median(0,0,5,5))
    }

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
