package geotrellis.feature.spec

import geotrellis.feature._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class PointSpec extends FunSpec with ShouldMatchers {
  describe("Point") {
    it("should return true for comparing points with equal x and y") {
      val x = 123.321
      val y = -0.4343434
      Point(x,y) should be (Point(x,y))
    }
  }
}

