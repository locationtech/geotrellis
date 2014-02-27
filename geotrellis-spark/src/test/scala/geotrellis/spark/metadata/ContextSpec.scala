package geotrellis.spark.metadata
import geotrellis.RasterExtent
import geotrellis.raster.TileLayout
import geotrellis.spark.TestEnvironment
import geotrellis.spark.testfiles.AllOnes
import geotrellis.spark.tiling.TmsTiling

import org.scalatest.matchers.ShouldMatchers

class ContextSpec extends TestEnvironment with ShouldMatchers {
  val allOnes = AllOnes(inputHome, conf)
  val meta = allOnes.meta
  val te = meta.metadataForBaseZoom.tileExtent
  val layout = allOnes.tileLayout
  val rasterExtent = allOnes.rasterExtent
  val zoom = meta.maxZoomLevel
  val tileSize = meta.tileSize
  val res = TmsTiling.resolution(zoom, tileSize)

  describe("RasterMetadata to RasterDefinition conversion tests") {
    it("should have the correct tile layout") {
      layout should be(TileLayout(te.width.toInt, te.height.toInt, tileSize, tileSize))
    }
    it("should have the correct raster extent") {
      val e = TmsTiling.tileToExtent(te, zoom, tileSize)
      val cols = tileSize * te.width
      val rows = tileSize * te.height
      rasterExtent should be(RasterExtent(e, res, res, cols.toInt, rows.toInt))
    }   
  }
  describe("RasterDefinition to RasterMetadata conversion tests") {
    it("should have the original raster metadata") {
      val e = TmsTiling.tileToExtent(te, zoom, tileSize)
      val pe = TmsTiling.extentToPixel(e, zoom, tileSize)
      val newMeta = allOnes.opCtx.toMetadata
      newMeta.extent should be(e)
      newMeta.tileSize should be(tileSize)
      newMeta.bands should be(PyramidMetadata.MaxBands)
      newMeta.awtRasterType should be(meta.awtRasterType)
      newMeta.maxZoomLevel should be(meta.maxZoomLevel)      
      
      // we can't just use meta.metadataForBaseZoom since pixelExtent there corresponds to the 
      // original image and not those of the tile boundaries
      newMeta.rasterMetadata should be(Map(meta.maxZoomLevel.toString -> RasterMetadata(pe,te)))
    }
  }
}