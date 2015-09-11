package geotrellis.spark.tiling

import geotrellis.proj4.LatLng
import geotrellis.raster.CellSize
import geotrellis.vector.Extent
import org.scalatest._

class FloatingLayoutSchemeSpec extends FunSpec with Matchers {
  describe("FloatingLayoutScheme"){
    val scheme: LayoutScheme = FloatingLayoutScheme(10)
    val level = scheme.levelFor(Extent(0,0,37,27), CellSize(1,1))

    it("should pad the layout to match source resolution"){
      assert(level.layout.tileLayout.totalCols === 40)
      assert(level.layout.tileLayout.totalRows === 30)
    }

    it("should expand the extent to cover padded pixels"){
      assert(level.layout.extent === Extent(0,0,40,30))
    }

    it("should have enough tiles to cover the source extent"){
      assert(level.layout.layoutCols ===  4)
      assert(level.layout.layoutRows ===  3)
    }
  }
}
