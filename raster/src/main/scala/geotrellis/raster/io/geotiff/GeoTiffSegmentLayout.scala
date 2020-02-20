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

import geotrellis.raster.{GridBounds, RasterExtent, TileLayout, PixelIsArea, Dimensions}
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector.{Extent, Geometry}

import spire.syntax.cfor._
import scala.collection.mutable

/**
  * This case class represents how the segments in a given [[GeoTiff]] are arranged.
  *
  * @param totalCols          The total amount of cols in the GeoTiff
  * @param totalRows          The total amount of rows in the GeoTiff
  * @param tileLayout         The [[TileLayout]] of the GeoTiff
  * @param storageMethod      Storage method used for the segments (tiled or striped)
  * @param interleaveMethod   The interleave method used for segments (pixel or band)
  */
case class GeoTiffSegmentLayout(
  totalCols: Int,
  totalRows: Int,
  tileLayout: TileLayout,
  storageMethod: StorageMethod,
  interleaveMethod: InterleaveMethod
) {
  def isTiled: Boolean =
    storageMethod match {
      case _: Tiled => true
      case _ => false
    }

  def isStriped: Boolean = !isTiled

  def hasPixelInterleave: Boolean = interleaveMethod == PixelInterleave

  /**
    * Finds the corresponding segment index given GeoTiff col and row.
    * If this is a band interleave geotiff, returns the segment index
    * for the first band.
    *
    * @param col  Pixel column in overall layout
    * @param row  Pixel row in overall layout
    * @return     The index of the segment in this layout
    */
  private [geotiff] def getSegmentIndex(col: Int, row: Int): Int = {
    val layoutCol = col / tileLayout.tileCols
    val layoutRow = row / tileLayout.tileRows
    (layoutRow * tileLayout.layoutCols) + layoutCol
  }

  /** Partition a list of pixel windows to localize required segment reads.
    * Some segments may be required by more than one partition.
    * Pixel windows outside of layout range will be filtered.
    * Maximum partition size may be exceeded if any window size exceeds it.
    * Windows will not be split to satisfy partition size limits.
    *
    * @param windows List of pixel windows from this layout
    * @param maxPartitionSize Maximum pixel count for each partition
    */
  def partitionWindowsBySegments(windows: Seq[GridBounds[Int]], maxPartitionSize: Long): Array[Array[GridBounds[Int]]] = {
    val partition = mutable.ArrayBuilder.make[GridBounds[Int]]
    partition.sizeHintBounded(128, windows)
    var partitionSize: Long = 0l
    var partitionCount: Long = 0l
    val partitions = mutable.ArrayBuilder.make[Array[GridBounds[Int]]]

    def finalizePartition() {
      val res = partition.result
      if (res.nonEmpty) partitions += res
      partition.clear()
      partitionSize = 0l
      partitionCount = 0l
    }

    def addToPartition(window: GridBounds[Int]) {
      partition += window
      partitionSize += window.size
      partitionCount += 1
    }

    val sourceBounds = GridBounds(0, 0, totalCols - 1, totalRows - 1)

    // Because GeoTiff segment indecies are enumorated in row-major order
    // sorting windows by the min index also provides spatial order
    val sorted = windows
      .filter(sourceBounds.intersects)
      .map { window =>
        window -> getSegmentIndex(col = window.colMin, row = window.rowMin)
      }.sortBy(_._2)

    for ((window, _) <- sorted) {
      if ((partitionCount == 0) || (partitionSize + window.size) < maxPartitionSize) {
        addToPartition(window)
      } else {
        finalizePartition()
        addToPartition(window)
      }
    }

    finalizePartition()
    partitions.result
  }

  private def bestWindowSize(maxSize: Int, segment: Int): Int = {
    var i: Int = 1
    var result: Int = -1
    // Search for the largest factor of segment that is > 1 and <=
    // maxSize.  If one cannot be found, give up and return maxSize.
    while (i < math.sqrt(segment) && result == -1) {
      if ((segment % i == 0) && ((segment/i) <= maxSize)) result = (segment/i)
      i += 1
    }
    if (result == -1) maxSize; else result
  }

  def listWindows(maxSize: Int): Array[GridBounds[Int]] = {
    val segCols = tileLayout.tileCols
    val segRows = tileLayout.tileRows

    val colSize: Int =
      if (maxSize >= segCols * 2) {
        math.floor(maxSize.toDouble / segCols).toInt * segCols
      } else if (maxSize >= segCols) {
        segCols
      } else bestWindowSize(maxSize, segCols)

    val rowSize: Int =
      if (maxSize >= segRows * 2) {
        math.floor(maxSize.toDouble / segRows).toInt * segRows
      } else if (maxSize >= segRows) {
        segRows
      } else bestWindowSize(maxSize, segRows)

    val windows = listWindows(colSize, rowSize)

    windows
  }

  /** List all pixel windows that meet the given geometry */
  def listWindows(maxSize: Int, extent: Extent, geometry: Geometry): Array[GridBounds[Int]] = {
    val segCols = tileLayout.tileCols
    val segRows = tileLayout.tileRows

    val maxColSize: Int =
      if (maxSize >= segCols * 2) {
        math.floor(maxSize.toDouble / segCols).toInt * segCols
      } else if (maxSize >= segCols) {
        segCols
      } else bestWindowSize(maxSize, segCols)

    val maxRowSize: Int =
      if (maxSize >= segRows) {
        math.floor(maxSize.toDouble / segRows).toInt * segRows
      } else if (maxSize >= segRows) {
        segRows
      } else bestWindowSize(maxSize, segRows)

    val result = scala.collection.mutable.Set.empty[GridBounds[Int]]
    val re = RasterExtent(extent, math.max(totalCols / maxColSize,1), math.max(totalRows / maxRowSize,1))
    val options = Rasterizer.Options(includePartial=true, sampleType=PixelIsArea)

    Rasterizer.foreachCellByGeometry(geometry, re, options)({ (col: Int, row: Int) =>
      result +=
        GridBounds(
          col * maxColSize,
          row * maxRowSize,
          math.min((col + 1) * maxColSize - 1, totalCols - 1),
          math.min((row + 1) * maxRowSize - 1, totalRows - 1)
        )
    })
    result.toArray
  }

  /** List all pixel windows that cover a grid of given size */
  def listWindows(cols: Int, rows: Int): Array[GridBounds[Int]] = {
    val result = scala.collection.mutable.ArrayBuilder.make[GridBounds[Int]]
    result.sizeHint((totalCols / cols) * (totalRows / rows))

    cfor(0)(_ < totalCols, _ + cols) { col =>
      cfor(0)(_ < totalRows, _ + rows) { row =>
        result +=
          GridBounds(
            col,
            row,
            math.min(col + cols - 1, totalCols - 1),
            math.min(row + rows - 1, totalRows - 1)
          )
      }
    }
    result.result
  }

  def bandSegmentCount: Int =
    tileLayout.layoutCols * tileLayout.layoutRows

  def getSegmentCoordinate(segmentIndex: Int): (Int, Int) =
    (segmentIndex % tileLayout.layoutCols, segmentIndex / tileLayout.layoutCols)

  /**
    * Calculates pixel dimensions of a given segment in this layout.
    * Segments are indexed in row-major order relative to the GeoTiff they comprise.
    *
    * @param segmentIndex: An Int that represents the given segment in the index
    * @return Tuple representing segment (cols, rows)
    */
  def getSegmentDimensions(segmentIndex: Int): Dimensions[Int] = {
    val normalizedSegmentIndex = segmentIndex % bandSegmentCount
    val layoutCol = normalizedSegmentIndex % tileLayout.layoutCols
    val layoutRow = normalizedSegmentIndex / tileLayout.layoutCols

    val cols =
      if(layoutCol == tileLayout.layoutCols - 1) {
        totalCols - ((tileLayout.layoutCols - 1) * tileLayout.tileCols)
      } else {
        tileLayout.tileCols
      }

    val rows =
      if(layoutRow == tileLayout.layoutRows - 1) {
        totalRows - ((tileLayout.layoutRows - 1) * tileLayout.tileRows)
      } else {
        tileLayout.tileRows
      }

    Dimensions(cols, rows)
  }

  private [geotrellis] def getGridBounds(segmentIndex: Int): GridBounds[Int] = {
    val normalizedSegmentIndex = segmentIndex % bandSegmentCount
    val Dimensions(segmentCols, segmentRows) = getSegmentDimensions(segmentIndex)

    val (startCol, startRow) = {
      val (layoutCol, layoutRow) = getSegmentCoordinate(normalizedSegmentIndex)
      (layoutCol * tileLayout.tileCols, layoutRow * tileLayout.tileRows)
    }

    val endCol = (startCol + segmentCols) - 1
    val endRow = (startRow + segmentRows) - 1

    GridBounds(startCol, startRow, endCol, endRow)
  }
}

trait GeoTiffSegmentLayoutTransform {
  private [geotrellis] def segmentLayout: GeoTiffSegmentLayout
  private lazy val GeoTiffSegmentLayout(totalCols, totalRows, tileLayout, isTiled, interleaveMethod) =
    segmentLayout

  /** Count of the bands in the GeoTiff */
  def bandCount: Int

  /** Calculate the number of segments per band */
  private def bandSegmentCount: Int =
    tileLayout.layoutCols * tileLayout.layoutRows

  /**
    * Calculates pixel dimensions of a given segment in this layout.
    * Segments are indexed in row-major order relative to the GeoTiff they comprise.
    *
    * @param segmentIndex: An Int that represents the given segment in the index
    * @return Tuple representing segment (cols, rows)
    */
  def getSegmentDimensions(segmentIndex: Int): Dimensions[Int] = {
    val normalizedSegmentIndex = segmentIndex % bandSegmentCount
    val layoutCol = normalizedSegmentIndex % tileLayout.layoutCols
    val layoutRow = normalizedSegmentIndex / tileLayout.layoutCols

    val cols =
      if(layoutCol == tileLayout.layoutCols - 1) {
        totalCols - ((tileLayout.layoutCols - 1) * tileLayout.tileCols)
      } else {
        tileLayout.tileCols
      }

    val rows =
      if(layoutRow == tileLayout.layoutRows - 1) {
        totalRows - ((tileLayout.layoutRows - 1) * tileLayout.tileRows)
      } else {
        tileLayout.tileRows
      }

    Dimensions(cols, rows)
  }

  /**
    * Calculates the total pixel count for given segment in this layout.
    *
    * @param segmentIndex: An Int that represents the given segment in the index
    * @return Pixel size of the segment
    */
  def getSegmentSize(segmentIndex: Int): Int = {
    val Dimensions(cols, rows) = getSegmentDimensions(segmentIndex)
    cols * rows
  }

  /**
    * Finds the corresponding segment index given GeoTiff col and row.
    * If this is a band interleave geotiff, returns the segment index
    * for the first band.
    *
    * @param col  Pixel column in overall layout
    * @param row  Pixel row in overall layout
    * @return     The index of the segment in this layout
    */
  private [geotiff] def getSegmentIndex(col: Int, row: Int): Int =
    segmentLayout.getSegmentIndex(col, row)

  private [geotiff] def getSegmentTransform(segmentIndex: Int): SegmentTransform = {
    val id = segmentIndex % bandSegmentCount
    if (segmentLayout.isStriped)
      StripedSegmentTransform(id, GeoTiffSegmentLayoutTransform(segmentLayout, bandCount))
    else
      TiledSegmentTransform(id, GeoTiffSegmentLayoutTransform(segmentLayout, bandCount))
  }

  def getSegmentCoordinate(segmentIndex: Int): (Int, Int) =
    (segmentIndex % tileLayout.layoutCols, segmentIndex / tileLayout.layoutCols)

  private [geotrellis] def getGridBounds(segmentIndex: Int): GridBounds[Int] = {
    val normalizedSegmentIndex = segmentIndex % bandSegmentCount
    val Dimensions(segmentCols, segmentRows) = getSegmentDimensions(segmentIndex)

    val (startCol, startRow) = {
      val (layoutCol, layoutRow) = getSegmentCoordinate(normalizedSegmentIndex)
      (layoutCol * tileLayout.tileCols, layoutRow * tileLayout.tileRows)
    }

    val endCol = (startCol + segmentCols) - 1
    val endRow = (startRow + segmentRows) - 1

    GridBounds(startCol, startRow, endCol, endRow)
  }

  /** Returns all segment indices which intersect given pixel grid bounds */
  private [geotrellis] def getIntersectingSegments(bounds: GridBounds[Int]): Array[Int] = {
    val colMax = totalCols - 1
    val rowMax = totalRows - 1
    val intersects = !(colMax < bounds.colMin || bounds.colMax < 0) && !(rowMax < bounds.rowMin || bounds.rowMax < 0)

    if (intersects) {
      val tc = tileLayout.tileCols
      val tr = tileLayout.tileRows
      val colMin = math.max(0, bounds.colMin)
      val rowMin = math.max(0, bounds.rowMin)
      val colMax = math.min(totalCols - 1, bounds.colMax)
      val rowMax = math.min(totalRows -1, bounds.rowMax)
      val ab = mutable.ArrayBuilder.make[Int]

      cfor(colMin / tc)(_ <= colMax / tc, _ + 1) { layoutCol =>
        cfor(rowMin / tr)(_ <= rowMax / tr, _ + 1) { layoutRow =>
          ab += (layoutRow * tileLayout.layoutCols) + layoutCol
        }
      }

      ab.result
    } else {
      Array.empty[Int]
    }
  }

  /** Partition a list of pixel windows to localize required segment reads.
    * Some segments may be required by more than one partition.
    * Pixel windows outside of layout range will be filtered.
    * Maximum partition size may be exceeded if any window size exceeds it.
    * Windows will not be split to satisfy partition size limits.
    *
    * @param windows List of pixel windows from this layout
    * @param maxPartitionSize Maximum pixel count for each partition
    */
  def partitionWindowsBySegments(windows: Seq[GridBounds[Int]], maxPartitionSize: Long): Array[Array[GridBounds[Int]]] =
    segmentLayout.partitionWindowsBySegments(windows, maxPartitionSize)

  /** Returns all segment indices which intersect given pixel grid bounds,
    * and for a subset of bands.
    * In a band interleave geotiff, generates the segment indices for the first band.
    *
    * @return  An array of (band index, segment index) tuples.
    */
  private [geotiff] def getIntersectingSegments(bounds: GridBounds[Int], bands: Array[Int]): Array[(Int, Int)] = {
    val firstBandSegments = getIntersectingSegments(bounds)
    bands.flatMap { band =>
      val segmentOffset = bandSegmentCount * band
      firstBandSegments.map { i => (band, i + segmentOffset) }
    }
  }
}

object GeoTiffSegmentLayoutTransform {
  def apply(_segmentLayout: GeoTiffSegmentLayout, _bandCount: Int): GeoTiffSegmentLayoutTransform =
    new GeoTiffSegmentLayoutTransform {
      val segmentLayout = _segmentLayout
      val bandCount = _bandCount
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
  def apply(
    totalCols: Int,
    totalRows: Int,
    storageMethod: StorageMethod,
    interleaveMethod: InterleaveMethod,
    bandType: BandType
  ): GeoTiffSegmentLayout = {
    val tileLayout =
      storageMethod match {
        case Tiled(blockCols, blockRows) =>
          val layoutCols = math.ceil(totalCols.toDouble / blockCols).toInt
          val layoutRows = math.ceil(totalRows.toDouble / blockRows).toInt
          TileLayout(layoutCols, layoutRows, blockCols, blockRows)
        case s: Striped =>
          val rowsPerStrip = math.min(s.rowsPerStrip(totalRows, bandType), totalRows).toInt
          val layoutRows = math.ceil(totalRows.toDouble / rowsPerStrip).toInt
          TileLayout(1, layoutRows, totalCols, rowsPerStrip)

      }
    GeoTiffSegmentLayout(totalCols, totalRows, tileLayout, storageMethod, interleaveMethod)
  }
}
