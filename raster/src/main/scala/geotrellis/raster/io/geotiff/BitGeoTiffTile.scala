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

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val result = BitArrayTile.empty(gridBounds.width, gridBounds.height)

    val colMin = gridBounds.colMin
    val rowMin = gridBounds.rowMin
    val tileCols = segmentLayout.tileLayout.tileCols
    val tileRows = segmentLayout.tileLayout.tileRows

    cfor(0)(_ < segmentCount, _ + 1) { i =>
      val segmentTransform = segmentLayout.getSegmentTransform(i)
      val colStart = segmentTransform.bitIndexToCol(0)
      val rowStart = segmentTransform.bitIndexToRow(0)
      val colEnd = (colStart + tileCols).min(cols)
      val rowEnd = (rowStart + tileRows).min(rows)

      if (gridBounds.intersects(GridBounds(colStart, rowStart, colEnd, rowEnd))) {
        cfor(colStart)(_ < colEnd, _ + 1) { col =>
          cfor(rowStart)(_ < rowEnd, _ + 1) { row =>
            if (gridBounds.contains(col, row))
              result.set(col - colMin, row - rowMin, get(col, row))
          }
        }
      }
    }
    result
  }


  def withNoData(noDataValue: Option[Double]): BitGeoTiffTile =
    new BitGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: BitCells with NoDataHandling =>
        new BitGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
