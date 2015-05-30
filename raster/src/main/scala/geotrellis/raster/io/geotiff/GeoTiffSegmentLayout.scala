package geotrellis.raster.io.geotiff

import geotrellis.raster.TileLayout

/** Specifically for single band segments. If dealing with multiband segments, you must do the math */
trait GridIndexTransform {
  def segmentCols: Int
  def segmentRows: Int

  /** The col of the source raster that this index represents. Can produce invalid cols */
  def indexToCol(i: Int): Int
  /** The row of the source raster that this index represents. Can produce invalid rows */
  def indexToRow(i: Int): Int

  /** Specific to BitGeoTiffSegment index. The col of the source raster that this index represents. */
  def bitIndexToCol(i: Int): Int
  /** Specific to BitGeoTiffSegment index. The row of the source raster that this index represents. */
  def bitIndexToRow(i: Int): Int

  def gridToIndex(col: Int, row: Int): Int
}

case class GeoTiffSegmentLayout(totalCols: Int, totalRows: Int, tileLayout: TileLayout, isTiled: Boolean) {
  def storageMethod: StorageMethod =
    if(isTiled)
      Tiled(tileLayout.tileCols, tileLayout.tileRows)
    else
      Striped(tileLayout.tileRows)

  def isStriped: Boolean = !isTiled

  def getSegmentDimensions(segmentIndex: Int): (Int, Int) = {
    val layoutCol = segmentIndex % tileLayout.layoutCols
    val layoutRow = segmentIndex / tileLayout.layoutCols

    val cols = 
      if(layoutCol == tileLayout.layoutCols - 1) {
        totalCols - ( (tileLayout.layoutCols - 1) * tileLayout.tileCols)
      } else {
        tileLayout.tileCols
      }

    val rows =
      if(layoutRow == tileLayout.layoutRows - 1) {
        totalRows - ( (tileLayout.layoutRows - 1) * tileLayout.tileRows)
      } else {
        tileLayout.tileRows
      }
    (cols, rows)
  }

  def getSegmentSize(segmentIndex: Int): Int = {
    val (cols, rows) = getSegmentDimensions(segmentIndex)
    cols * rows
  }

  def getSegmentIndex(col: Int, row: Int): Int = {
    val layoutCol = col / tileLayout.tileCols
    val layoutRow = row / tileLayout.tileRows
    (layoutRow * tileLayout.layoutCols) + layoutCol
  }

  def getSegmentTransform(segmentIndex: Int): GridIndexTransform = {
    val layoutCol = segmentIndex % tileLayout.layoutCols
    val layoutRow = segmentIndex / tileLayout.layoutCols

    new GridIndexTransform {
      val segmentCols =
        if(layoutCol == tileLayout.layoutCols - 1) {
          totalCols - ( (tileLayout.layoutCols - 1) * tileLayout.tileCols)
        } else {
          tileLayout.tileCols
        }

      val segmentRows =
        if(layoutRow == tileLayout.layoutRows - 1) {
          totalRows - ( (tileLayout.layoutRows - 1) * tileLayout.tileRows)
        } else {
          tileLayout.tileRows
        }

      def indexToCol(i: Int) = {
        val tileCol = i % tileLayout.tileCols
        (layoutCol * tileLayout.tileCols) + tileCol
      }

      def indexToRow(i: Int) = {
        val tileRow = i / tileLayout.tileCols
        (layoutRow * tileLayout.tileRows) + tileRow
      }

      def bitIndexToCol(i: Int) = {
        val tileCol = i % segmentCols
        (layoutCol * tileLayout.tileCols) + tileCol
      }

      def bitIndexToRow(i: Int) = {
        val tileRow = i / segmentCols
        (layoutRow * tileLayout.tileRows) + tileRow
      }

      def gridToIndex(col: Int, row: Int): Int = {
        val tileCol = col - (layoutCol * tileLayout.tileCols)
        val tileRow = row - (layoutRow * tileLayout.tileRows)
        if(isTiled) { tileRow * tileLayout.tileCols + tileCol }
        else { tileRow * segmentCols + tileCol }
      }
    }
  }
}

object GeoTiffSegmentLayout {
  def apply(totalCols: Int, totalRows: Int, storageMethod: StorageMethod, bandType: BandType): GeoTiffSegmentLayout = {
    storageMethod match {
      case Tiled(blockCols, blockRows) =>
        val layoutCols = math.ceil(totalCols.toDouble / blockCols).toInt
        val layoutRows = math.ceil(totalRows.toDouble / blockRows).toInt
        val tileLayout = TileLayout(layoutCols, layoutRows, blockCols, blockRows)
        GeoTiffSegmentLayout(totalCols, totalRows, tileLayout, true)
      case s: Striped =>
        val rowsPerStrip = math.min(s.rowsPerStrip(totalRows, bandType), totalRows).toInt
        val layoutRows = math.ceil(totalRows.toDouble / rowsPerStrip).toInt
        val tileLayout = TileLayout(1, layoutRows, totalCols, rowsPerStrip)
        GeoTiffSegmentLayout(totalCols, totalRows, tileLayout, false)
    }
  }
}
