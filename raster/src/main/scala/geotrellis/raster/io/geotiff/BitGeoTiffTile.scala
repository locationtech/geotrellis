package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class BitGeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
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
}
