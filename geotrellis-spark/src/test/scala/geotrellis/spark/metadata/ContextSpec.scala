package geotrellis.spark.metadata
import geotrellis.RasterExtent
import geotrellis.raster.TileLayout
import geotrellis.spark.TestEnvironment
import geotrellis.spark.testfiles.AllOnes
import geotrellis.spark.tiling.TmsTiling
import org.scalatest.matchers.ShouldMatchers
import geotrellis.spark.TestEnvironmentFixture
import geotrellis.spark.rdd.RasterHadoopRDD
import org.apache.hadoop.fs.Path

/* 
 * This is not just "oldMeta should be(newMeta)" since newMeta is actually supposed to be 
 * different from oldMeta in two respects:
 * 1. newMeta.extent != oldMeta.extent
 * 2. newMeta.metadataForBaseZoom.pixelExtent != oldMeta.metadataForBaseZoom.pixelExtent
 */
trait MetadataMatcher extends ShouldMatchers {
  def shouldBe(oldMeta: PyramidMetadata, newMeta: PyramidMetadata) = {
    val oldTileSize = oldMeta.tileSize
    val oldZoom = oldMeta.maxZoomLevel
    val oldTe = oldMeta.metadataForBaseZoom.tileExtent
    val oldE = TmsTiling.tileToExtent(oldTe, oldZoom, oldTileSize)
    val oldPe = TmsTiling.extentToPixel(oldE, oldZoom, oldTileSize)
    newMeta.extent should be(oldE)
    newMeta.tileSize should be(oldTileSize)
    newMeta.bands should be(PyramidMetadata.MaxBands)
    newMeta.awtRasterType should be(oldMeta.awtRasterType)
    newMeta.maxZoomLevel should be(oldZoom)

    // we can't just use meta.metadataForBaseZoom since pixelExtent there corresponds to the 
    // original image and not those of the tile boundaries
    newMeta.rasterMetadata should be(Map(oldZoom.toString -> RasterMetadata(oldPe, oldTe)))

  }
}

/*
 * This is split into two classes as one mixes in TestEnvironmentFixture and the other mixes 
 * in TestEnvironment
 */ 
class ContextSpec extends TestEnvironment with MetadataMatcher {
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
      val newMeta = allOnes.opCtx.toMetadata
      shouldBe(meta, newMeta)
    }
  }
}

class ContextSaveSpec extends TestEnvironmentFixture with MetadataMatcher {
  val allOnes = AllOnes(inputHome, conf)

  describe("Passing Context through operations tests") {
    it("should produce the expected PyramidMetadata") { sc =>
      println("----------" + outputLocal + "------------")
      val ones = RasterHadoopRDD.toRasterRDD(allOnes.path, sc)
      val twos = ones + ones
      val twosPath = new Path(outputLocal, ones.opCtx.zoom.toString)
      mkdir(twosPath)
      twos.save(twosPath)

      val newMeta = PyramidMetadata(outputLocal, conf)
      shouldBe(allOnes.meta, newMeta)    
    }
  }
}