package geotrellis

import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RasterExtentSpec extends FunSpec with MustMatchers 
                                       with ShouldMatchers 
                                       with RasterBuilders {
  describe("A RasterExtent object") {
    val e1 = Extent(0.0, 0.0, 1.0, 1.0)
    val e2 = Extent(0.0, 0.0, 20.0, 20.0)

    val g1 = RasterExtent(e1, 1.0, 1.0, 1, 1)
    val g2 = RasterExtent(e2, 1.0, 1.0, 20, 20)
    val g3 = g1
    val g4 = RasterExtent(e1, 1.0, 1.0, 1, 1)
    
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

  def sampleRasterExtent = {
    val baseExtent = Extent(10,-100,30,0)
    RasterExtent(baseExtent,2,20,10,5)
  }

  describe("Getting grid bounds from an an extent") {
    it("should return whole grid when using it's own extent") {
      val rasterExtent = sampleRasterExtent
      val baseExtent = Extent(10,-100,30,0)
      val expected = GridBounds(0,0,rasterExtent.cols-1,rasterExtent.rows-1)
      rasterExtent.gridBoundsFor(baseExtent) should be (expected)
    }

    it("should get top left cell") {
      val rasterExtent = sampleRasterExtent
      val subExtent = Extent(10,-20,12,0)
      val expected = GridBounds(0,0,0,0)
      rasterExtent.gridBoundsFor(subExtent) should be (expected)
    }

    it("should get 2x2 subgrid from southwest corner") {
      val rasterExtent = sampleRasterExtent
      val subExtent = Extent(26,-100,30,-60)
      val expected = GridBounds(8,3,9,4)
      rasterExtent.gridBoundsFor(subExtent) should be (expected)      
    }

    it("should get bounds for extents that do not fall on grid lines") {
      // Map of subExtends to expected Grid Bounds
      val testData = Map( 
        (Extent(25,-92,29,-81),GridBounds(7,4,9,4)),
        (Extent(12.01,-42,24.5,-20.1),GridBounds(1,1,7,2))
      )
      val rasterExtent = sampleRasterExtent

      for((subExtent,expected) <- testData)
        rasterExtent.gridBoundsFor(subExtent) should be (expected)      
    }

    it("should handle subExtents that are out of bounds") {
      intercept[ExtentRangeError] {
        val rasterExtent = sampleRasterExtent
        val subExtent = Extent(-26,-100,30,-60)
        val expected = GridBounds(8,3,9,4)
        rasterExtent.gridBoundsFor(subExtent)
      }
    }
  }
}
