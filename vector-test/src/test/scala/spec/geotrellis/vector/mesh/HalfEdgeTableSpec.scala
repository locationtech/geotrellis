package geotrellis.vector.mesh

import org.scalatest.{FunSpec, Matchers}

class HalfEdgeTableSpec extends FunSpec with Matchers {
  describe ("HalfEdgeTable") {
    it ("should add and remove edges correctly") {
      val het = new HalfEdgeTable(5)

      for (i <- 0 to 3) {
        val ei = het.createHalfEdge(i)
        assert(ei == i)
      }
      het.killEdge(2)
      assert(het.createHalfEdge(12) == 2)
      assert(het.createHalfEdge(4) == 4)
      true should be (true)
    }
  }
}
