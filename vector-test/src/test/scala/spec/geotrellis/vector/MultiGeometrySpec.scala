package geotrellis.vector

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest._

class MultiGeometrySpec extends FunSpec with Matchers {
  describe("MultiGeometry.envelope") {
    it("should return a 0 Extent when empty.") {
      val env = Set(
        MultiPoint().envelope,
        MultiLine().envelope,
        MultiPolygon().envelope,
        GeometryCollection().envelope
      )

      env.size should be (1)
      env.toSeq.head should be (Extent(0.0, 0.0, 0.0, 0.0))
    }
  }
}
