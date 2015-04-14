package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector._

import org.scalatest._

class MapKeyTransformSpec extends FunSpec with Matchers {
  describe("MapKeyTransform") { 
    it("converts a grid bounds that is on the borders of the tile layout correctly") {
      val crs = LatLng
      val tileLayout = TileLayout(8, 8, 3, 4)
      val mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
      val gridBounds = GridBounds(1, 1, 6, 7)
      val extent = mapTransform(gridBounds)
      val result = mapTransform(extent)

      result should be (gridBounds)
    }
  }
}
