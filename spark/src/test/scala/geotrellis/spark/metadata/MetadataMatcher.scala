package geotrellis.spark.metadata
import org.scalatest._
import geotrellis.spark.tiling.TmsTiling


/* 
 * This is not just "oldMeta should be(newMeta)" since newMeta is actually supposed to be 
 * different from oldMeta in two respects:
 * 1. newMeta.extent != oldMeta.extent
 * 2. newMeta.metadataForBaseZoom.pixelExtent != oldMeta.metadataForBaseZoom.pixelExtent
 */
// trait MetadataMatcher extends ShouldMatchers {
//   def shouldBe(oldMeta: PyramidMetadata, newMeta: PyramidMetadata) = {
//     val oldTileSize = oldMeta.tileSize
//     val oldZoom = oldMeta.maxZoomLevel
//     val oldTe = oldMeta.metadataForBaseZoom.tileExtent
//     val oldE = TmsTiling.tileToExtent(oldTe, oldZoom, oldTileSize)
//     val oldPe = TmsTiling.extentToPixel(oldE, oldZoom, oldTileSize)
//     newMeta.extent should be(oldE)
//     newMeta.tileSize should be(oldTileSize)
//     newMeta.bands should be(PyramidMetadata.MaxBands)
//     newMeta.awtCellType should be(oldMeta.awtCellType)
//     newMeta.maxZoomLevel should be(oldZoom)

//     // we can't just use meta.metadataForBaseZoom since pixelExtent there corresponds to the 
//     // original image and not those of the tile boundaries
//     newMeta.rasterMetadata should be(Map(oldZoom.toString -> RasterMetadata(oldPe, oldTe)))

//   }
// }
