package geotrellis.spark.metadata
import geotrellis.RasterExtent
import geotrellis.raster.TileLayout
import geotrellis.spark.TestEnvironment
import geotrellis.spark.testfiles.AllOnes
import geotrellis.spark.tiling.TmsTiling

import org.scalatest.matchers.ShouldMatchers

class SparkRasterDefinitionSpec extends TestEnvironment with ShouldMatchers {
  val allOnes = AllOnes(inputHome, conf)
  val meta = allOnes.meta
  val te = meta.metadataForBaseZoom.tileExtent
  val layout = allOnes.rasterDefinition.tileLayout
  val rasterExtent = allOnes.rasterDefinition.rasterExtent
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
}