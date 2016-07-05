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

/**
 * This case class represents how the segments in a given [[GeoTiff]] are arranged.
 *
 * @param totalCols: The total amount of cols in the GeoTiff
 * @param totalRows: The total amount of rows in the GeoTiff
 * @param tileLayout: The [[TileLayout]] of the GeoTiff
 * @param isTiled: A Boolean that represents if the given GeoTiff is Tiled or not
 * @return A new instance of the GeoTiffSegmentLayout case class
 */
case class GeoTiffSegmentLayout(totalCols: Int, totalRows: Int, tileLayout: TileLayout, isTiled: Boolean) {
  /**
   * Determines if the GeoTiff uses Striped or Tiled storage of data
   *
   * @return Returns the [[StorageMethod]] of the GeoTiff
   */
  def storageMethod: StorageMethod =
    if(isTiled)
      Tiled(tileLayout.tileCols, tileLayout.tileRows)
    else
      Striped(tileLayout.tileRows)

  /** Determines if the GeoTiff has Striped storage*/
  def isStriped: Boolean = !isTiled

  /**
   * Finds the max number of cols and rows of a given [[GeoTiffSegment]]
   *
   * @param segmentIndex: An Int that represents the given segment in the index
   * @return The max cols and rows represented as (Int, Int)
   */
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

  /**
   * Returns the area of the given segment in terms of cells
   *
   * @param segmentIndex: An Int that represents the given segment in the index
   * @return The area of the segment in terms of cells
   */
  def getSegmentSize(segmentIndex: Int): Int = {
    val (cols, rows) = getSegmentDimensions(segmentIndex)
    cols * rows
  }

  /**
   * Finds the corresponding segment index given the col and row
   *
   * @param col: The specified col
   * @param row: The specified row
   * @return The index of the segment that the given position falls in
   */
  def getSegmentIndex(col: Int, row: Int): Int = {
    val layoutCol = col / tileLayout.tileCols
    val layoutRow = row / tileLayout.tileRows
    (layoutRow * tileLayout.layoutCols) + layoutCol
  }

  /**
   * Given a segment Index, return a [[GridIndexTransform]] object.
   *
   * @param segmentIndex: An Int that represents the index of the segment
   * @return A GridIndexTransform Object
   */
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

/**
 * The companion object of [[GeoTiffSegmentLayout]]
 */
object GeoTiffSegmentLayout {
  /**
   * Given the totalCols, totalRows, storageMethod, and BandType of a GeoTiff,
   * a new instance of GeoTiffSegmentLayout will be created
   *
   * @param totalCols: The total amount of cols in the GeoTiff
   * @param totalRows: The total amount of rows in the GeoTiff
   * @param storageMethod: The [[StorageMethod]] of the GeoTiff
   * @param bandType: The [[BandType]] of the GeoTiff
   * @return a new instance of the [[GeoTiffSegmentLayout]] case class
   */
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
