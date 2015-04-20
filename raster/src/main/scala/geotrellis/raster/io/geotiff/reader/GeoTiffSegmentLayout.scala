package geotrellis.raster.io.geotiff.reader

import geotrellis.raster.TileLayout
import geotrellis.raster.io.geotiff.tags._

import monocle.syntax._

trait GridIndexTransform {
  def segmentCols: Int
  def segmentRows: Int

  /** The col of the source raster that this index represents. Can produce invalid cols */
  def indexToCol(i: Int): Int
  /** The row of the source raster that this index represents. Can produce invalid rows */
  def indexToRow(i: Int): Int
  def gridToIndex(col: Int, row: Int): Int
}

class GeoTiffSegmentLayout(val totalCols: Int, val totalRows: Int, val tileLayout: TileLayout, isTiled: Boolean) {
  def isStriped: Boolean = tileLayout.layoutCols == 1

  def getSegmentSize(segmentIndex: Int): Int = {
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
        val tileRow = i / tileLayout.tileRows
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
  def apply(tags: Tags): GeoTiffSegmentLayout = {
    val totalCols = tags.cols
    val totalRows = tags.rows

    val tileLayout =
      // If tileWidth tag is present, we have a tiled geotiff.
      (tags
        &|-> Tags._tileTags
        ^|-> TileTags._tileWidth get) match {
        case Some(tileWidth) =>
          val tileHeight =
            (tags
              &|-> Tags._tileTags
              ^|-> TileTags._tileLength get).get
          TileLayout(
            math.ceil(totalCols.toDouble / tileWidth).toInt, 
            math.ceil(totalRows.toDouble / tileHeight).toInt, 
            tileWidth.toInt, 
            tileHeight.toInt)
        case None =>
          // Striped GeoTiff
          val rowsPerStrip: Int =
            (tags
              &|-> Tags._basicTags
              ^|-> BasicTags._rowsPerStrip get).toInt
          TileLayout(1, math.ceil(totalRows.toDouble / rowsPerStrip).toInt, totalCols, rowsPerStrip)
      }
    new GeoTiffSegmentLayout(totalCols, totalRows, tileLayout, !tags.hasStripStorage)
  }
}
