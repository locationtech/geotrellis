package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

import scala.collection.mutable._

class BitGeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: BitCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with BitGeoTiffSegmentCollection {
  // We need multiband information because BitGeoTiffSegments are unique
  val hasPixelInterleave = false

  // TODO: Optimize this.
  def mutable: MutableArrayTile = {
    val result = BitArrayTile.empty(cols, rows)

    val layoutCols = segmentLayout.tileLayout.layoutCols
    val tileCols = segmentLayout.tileLayout.tileCols

    val layoutRows = segmentLayout.tileLayout.layoutRows
    val tileRows = segmentLayout.tileLayout.tileRows

    cfor(0)(_ < layoutCols, _ + 1) { layoutCol =>
      val colStart = layoutCol * tileCols
      val colEnd = (colStart + tileCols).min(cols)
      cfor(0)(_ < layoutRows, _ + 1) { layoutRow =>
        val rowStart = layoutRow * tileRows
        val rowEnd = (rowStart + tileRows).min(rows)
        cfor(colStart)(_ < colEnd, _ + 1) { col =>
          cfor(rowStart)(_ < rowEnd, _ + 1) { row =>
            result.set(col, row, get(col, row))
          }
        }
      }
    }
   result
  }

  def mutable(windowedGeoTiff: WindowedGeoTiff): MutableArrayTile = {
    val windowedGridBounds = windowedGeoTiff.windowedGridBounds
    val intersectingSegments = windowedGeoTiff.intersectingSegments
    val result = BitArrayTile.empty(windowedGridBounds.width, windowedGridBounds.height)

    val colMin = windowedGridBounds.colMin
    val rowMin = windowedGridBounds.rowMin

    val tileCols = segmentLayout.tileLayout.tileCols
    val tileRows = segmentLayout.tileLayout.tileRows

    for (segmentId <- intersectingSegments) {
      val segmentTransform = segmentLayout.getSegmentTransform(segmentId)
      
      val colStart = segmentTransform.bitIndexToCol(0)
      val colEnd = (colStart + tileCols).min(cols)
      val rowStart = segmentTransform.bitIndexToRow(0)
      val rowEnd = (rowStart + tileRows).min(rows)

      cfor(colStart)(_ < colEnd, _ + 1) { col =>
        cfor(rowStart)(_ < rowEnd, _ + 1) { row =>
          if (windowedGridBounds.contains(col, row))
            result.set(col - colMin, row - rowMin, get(col, row))
        }
      }
    }
    result
  }
}
