package geotrellis.vector

import org.scalatest.FunSpec
import org.scalatest.Matchers

import geotrellis.vector.affine._

class GeometryResultSpec extends FunSpec with Matchers {
  describe("GeometryResult") {
    it("should return Some(Geometry) for intersection") {
      val p = Polygon(Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0))))
      val p2 = p.translate(0.5, 0.5)

      (p & p2).toGeometry.isDefined should be (true)
    }

    it("should return None for empty intersection") {
      val p = Polygon(Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0))))
      val p2 = p.translate(5.0, 5.0)
      (p & p2).toGeometry.isDefined should be (false)
    }

    it("should use asMultiLine to be able to union over a set of lines") {
      val lines = 
        Seq(
          Line((0,0), (2,2)),
          Line((1,1), (3,3)),
          Line((0,2), (2,0))
        )

      val result = 
        lines.foldLeft(None: Option[MultiLine]) { (union, line) =>
          union match {
            case Some(l1) => (l1 | line).asMultiLine
            case None => Some(MultiLine(line))
          }
        }
      result.isDefined should be (true)
      result.get should be (
        MultiLine(
          Line((0,0), (3,3)),
          Line((0,2), (2,0))
        )
      )
    }
  }
}
