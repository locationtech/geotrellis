package geotrellis.vector

import org.scalatest.FunSpec
import org.scalatest.Matchers

import geotrellis.vector.affine._

class GeometryResultSpec extends FunSpec with Matchers {
  describe("GeoetryResult") {
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
  }
}
