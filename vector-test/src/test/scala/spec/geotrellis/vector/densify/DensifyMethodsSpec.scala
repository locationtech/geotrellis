package geotrellis.vector.densify

import geotrellis.vector._
import geotrellis.vector.testkit._

import spire.syntax.cfor._

import org.scalatest._

class DensifyMethodsSpec extends FunSpec
    with Matchers {
  describe("Densify") {
    it("should handle an empty multiline") {
      val ml = MultiLine()
      ml.densify(20) should be (ml)
    }

    it("should handle an non-empty multiline") {
      val l1 = Line((0, 0), (2, 0))
      val l2 = Line((0, 0), (0, -2))
      val ml = MultiLine(l1, l2)

      val el1 = Line((0, 0), (1, 0), (2, 0))
      val el2 = Line((0, 0), (0, -1), (0, -2))
      val expected = MultiLine(el1, el2)
      val actual = ml.densify(1.1)

      actual should matchGeom(expected)
    }

    it("should handle a geometryCollection") {
      val l1 = Line((0, 0), (2, 0))
      val l2 = Line((0, 0), (0, -2))
      val ml = MultiLine(l1, l2)

      val el1 = Line((0, 0), (1, 0), (2, 0))
      val el2 = Line((0, 0), (0, -1), (0, -2))
      val expectedMl = MultiLine(el1, el2)

      val actual = GeometryCollection(multiLines = Seq(ml), points = Seq(Point(0, 0))).densify(1.1)

      actual should matchGeom(GeometryCollection(multiLines = Seq(expectedMl), points = Seq(Point(0, 0))))
    }
  }
}
