package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class LayoutDefinitionSpec extends FunSpec with Matchers {
  describe("LayoutDefinition"){
    it("should not buffer the extent of a grid that fits within it's bounds"){
      val e = Extent(-31.4569758,  27.6350020, 40.2053192,  80.7984255)
      val cs = CellSize(0.08332825, 0.08332825)
      val tileSize = 256
      val ld = LayoutDefinition(GridExtent(e, cs), tileSize, tileSize)
      val ld2 = LayoutDefinition(GridExtent(ld.extent, cs), ld.tileCols, ld.tileRows)

      ld2.extent should be (ld.extent)
    }
  }
}
