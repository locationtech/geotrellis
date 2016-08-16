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

    it("should find k-Nearest Neighbors correctly") {
      val pts = for (i <- -100 to 100;
                     j <- -100 to 100;
                     if i != j
                    ) yield (i,j)

      val idx = geotrellis.vector.SpatialIndex(pts){ p => (p._1.toFloat, p._2.toFloat) }

      val res = idx.kNearest ((0.0,0.0), 18)

      val expected = List(        (-1, 2),(0, 2),(1, 2),
                          (-2, 1),(-1, 1),(0, 1),       (2, 1),
                          (-2, 0),(-1, 0),       (1, 0),(2, 0),
                          (-2,-1),        (0,-1),(1,-1),(2,-1),
                                  (-1,-2),(0,-2),(1,-2))

      val resinex = res.forall { x => expected contains x }
      val exinres = expected.forall { x => res contains x }

      (resinex && exinres) should be (true)
    }
  }
}
