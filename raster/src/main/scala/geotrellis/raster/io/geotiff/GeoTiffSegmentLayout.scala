/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff

import geotrellis.raster.GridBounds
import geotrellis.raster.TileLayout
import scala.collection.mutable.ArrayBuffer

/**
 * This case class represents how the segments in a given [[GeoTiff]] are arranged.
 *
 * @param totalCols  The total amount of cols in the GeoTiff
 * @param totalRows  The total amount of rows in the GeoTiff
 * @param tileLayout The [[TileLayout]] of the GeoTiff
 * @param isTiled    A Boolean that represents if the given GeoTiff is Tiled or not
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
   * Calculates pixel dimensions of a given segment in this layout.
   * Segments are indexed in row-major order relative to the GeoTiff they comprise.
   *
   * @param segmentIndex: An Int that represents the given segment in the index
   * @return Tuple representing segment (cols, rows)
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
   * Calculates the total pixel count for given segment in this layout.
   *
   * @param segmentIndex: An Int that represents the given segment in the index
   * @return Pixel size of the segment
   */
  def getSegmentSize(segmentIndex: Int): Int = {
    val (cols, rows) = getSegmentDimensions(segmentIndex)
    cols * rows
  }

  /**
   * Finds the corresponding segment index given GeoTiff col and row
   *
   * @param col  Pixel column in overall layout
   * @param row  Pixel row in overall layout
   * @return     The index of the segment in this layout
   */
  def getSegmentIndex(col: Int, row: Int): Int = {
    val layoutCol = col / tileLayout.tileCols
    val layoutRow = row / tileLayout.tileRows
    (layoutRow * tileLayout.layoutCols) + layoutCol
  }

  def getSegmentCoordinate(segmentIndex: Int): (Int, Int) =
    (segmentIndex % tileLayout.layoutCols, segmentIndex / tileLayout.layoutCols)

  private [geotiff] def getSegmentTransform(segmentIndex: Int, bandCount: Int): SegmentTransform =
    if (isStriped)
      StripedSegmentTransform(segmentIndex, bandCount, this)
    else
      TiledSegmentTransform(segmentIndex, bandCount, this)

  def getGridBounds(segmentIndex: Int, isBit: Boolean = false): GridBounds = {
    val (segmentCols, segmentRows) = getSegmentDimensions(segmentIndex)

    val (startCol, startRow) = {
      val (layoutCol, layoutRow) = getSegmentCoordinate(segmentIndex)
      (layoutCol * tileLayout.tileCols, layoutRow * tileLayout.tileRows)
    }

    val endCol = (startCol + segmentCols) - 1
    val endRow = (startRow + segmentRows) - 1

    GridBounds(startCol, startRow, endCol, endRow)
  }

  /** Returns all segment indices which intersect given pixel grid bounds */
  def intersectingSegments(bounds: GridBounds): Array[Int] = {
    val tc = tileLayout.tileCols
    val tr = tileLayout.tileRows
    val ab = ArrayBuffer[Int]()
    for (layoutCol <- (bounds.colMin / tc) to (bounds.colMax / tc)) {
      for (layoutRow <- (bounds.rowMin / tr) to (bounds.rowMax / tr)) {
        ab += (layoutRow * tileLayout.layoutCols) + layoutCol
      }
    }
    ab.toArray
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
