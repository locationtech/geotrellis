package geotrellis.vector

import org.scalatest.FunSpec
import org.scalatest.Matchers

class SpatialIndexSpec extends FunSpec with Matchers {
  describe("SpatialIndex") {
    it("should find correct points in extents") {
      val vs = 
        List( (10.0,0.0),
          (-0.5,-0.5),
          (-13.0,-3.0),
          (13.0,3.0),
          (-10.0,0.0),
          (0.0,0.0))

      val index =
        geotrellis.vector.SpatialIndex(0 until vs.size) { i =>
          val v = vs(i)
          (v._1, v._2)
        }

      val extents = 
        Seq(
          Extent(-15.0,-5.0,-5.0,5.0),
          Extent(-5.0,-5.0,5.0,5.0),
          Extent(5.0,-5.0,15.0,5.0)
        )

      for(extent <- extents) {
        val expected = vs.filter { v => extent.contains(v) }
        val actual = index.pointsInExtent(extent).map { i => vs(i) }
        actual should be (expected)
      }
    }
  }
}
