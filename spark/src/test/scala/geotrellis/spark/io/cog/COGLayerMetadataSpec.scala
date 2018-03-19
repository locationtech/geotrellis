package geotrellis.spark.io.cog

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.vector._

import org.scalatest._
import spray.json._

class COGLayerMetadataSpec extends FunSpec
  with Matchers {

  describe("COGLayerMetadata.apply") {
    it("should produce correct metadata for landsat scene example") {

      val cellType = IntConstantNoDataCellType
      val extent = Extent(1.5454921707580032E7, 4146985.8273180854, 1.5762771754498206E7, 4454374.804079672)
      val crs = WebMercator
      val keyBounds = KeyBounds(SpatialKey(7255,3185),SpatialKey(7318,3248))
      val layoutScheme = ZoomedLayoutScheme(WebMercator)
      val maxZoom = 13
      val minZoom = 0
      val maxTileSize = 4096

      val md = COGLayerMetadata(cellType, extent, crs, keyBounds, layoutScheme, maxZoom, minZoom, maxTileSize)
      println(md.toJson.prettyPrint)
      md.zoomRanges.isEmpty should be (false)
    }
  }
}
