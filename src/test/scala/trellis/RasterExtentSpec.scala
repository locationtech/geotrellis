package geotrellis

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
//import org.junit.runner.RunWith

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RasterExtentSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("A RasterExtent object") {
    val e1 = Extent(0.0, 0.0, 1.0, 1.0)
    val e2 = Extent(0.0, 0.0, 20.0, 20.0)

    val g1 = RasterExtent(e1, 1.0, 1.0, 1, 1)
    val g2 = RasterExtent(e2, 1.0, 1.0, 20, 20)
    val g3 = g1
    val g4 = RasterExtent(e1, 1.0, 1.0, 1, 1)

    g4.cellheight
    
    it("should compare") {
      g1.compare(g2) must be === -1
      g1.compare(g3) must be === 0
      g1.compare(g4) must be === 0
    }

    it("should be able to do contains") {
      g1.containsPoint(0.5, 0.5) must be === true
    }

    it("should stringify") {
      val s = g1.toString
    }

    it("should die when invalid #1") {
      evaluating {
        RasterExtent(e1, 1.0, 1.0, -10, 10)
      } should produce [Exception];
    }

    it("should die when invalid #2") {
      evaluating {
        RasterExtent(e1, 1.0, 1.0, 10, -10)
      } should produce [Exception];
    }

    it("should die when invalid #3") {
      evaluating {
        RasterExtent(e1, 0.0, 1.0, 0, 10)
      } should produce [Exception];
    }

    it("should die when invalid #4") {
      evaluating {
        RasterExtent(e1, 1.0, -1.0, 10, -10)
      } should produce [Exception];
    }

    val g = RasterExtent(Extent(10.0, 15.0, 90.0, 95.0), 2.0, 2.0, 40, 40)

    it("should convert map coordinates to grid") {
      g.mapToGrid(10.0, 15.0) must be === (0, 40)
      g.mapToGrid(89.9, 95.1) must be === (39, 0)
      g.mapToGrid(33.1, 61.6) must be === (11, 16)
    }

    it("should convert grid coordinates to map") {
      g.gridToMap(0, 0) must be === (11.0, 94.0)
      g.gridToMap(39, 39) must be === (89.0, 16.0)
      g.gridToMap(12, 23) must be === (35.0, 48.0)
    }

    it("should combine correctly") {
      g1.combine(g2) must be === g2

      evaluating {
        g1.combine(RasterExtent(e1, 4.0, 4.0, 5, 5))
      } should produce [Exception];
    }
  }
}
