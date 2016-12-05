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

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

import scala.collection.mutable._

class UByteGeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: UByteCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with UByteGeoTiffSegmentCollection {

  val noDataValue: Option[Int] = cellType match {
    case UByteCellType => None
    case UByteConstantNoDataCellType => Some(0)
    case UByteUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          getSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols
        val tileWidth = segmentLayout.tileLayout.tileCols

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          val j = (row * cols) + col
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    UByteArrayTile.fromBytes(arr, cols, rows, cellType)
  }
  
  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Byte](gridBounds.size)
    var counter = 0
		var c = 0

		val segments = segmentBytes.intersectingSegments

		if (segmentLayout.isStriped) {
      cfor(0)(_ < segments.length, _ + 1) { i =>
				val segmentId = segments(i)
        val segmentGridBounds = segmentLayout.getGridBounds(segmentId)
				val segment = getSegment(segmentId)
				
				val result =
					gridBounds.intersection(segmentGridBounds).get
				val intersection =
					Intersection(segmentGridBounds, result, segmentLayout)

				cfor(intersection.start)(_ < intersection.end, _ + cols) { i =>
					System.arraycopy(segment.bytes, i, arr, counter, result.width)
					counter += result.width
				}
      }
    } else {
      cfor(0)(_ < segments.length, _ + 1) { i =>
				val segmentId = segments(i)
        val segmentGridBounds = segmentLayout.getGridBounds(segmentId)
				val segment = getSegment(segmentId)

				val segmentTransform = segmentLayout.getSegmentTransform(segmentId)
				val tileWidth = segmentLayout.tileLayout.tileCols

				val result =
					gridBounds.intersection(segmentGridBounds).get

				val intersection =
					Intersection(segmentGridBounds, result, segmentLayout)

				cfor(intersection.start)(_ < intersection.end, _ + tileWidth) { i =>
					val col = segmentTransform.indexToCol(i)
					val row = segmentTransform.indexToRow(i)
					val j = (row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)
					System.arraycopy(segment.bytes, i, arr, j, result.width)
				}
      }
    }
    UByteArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }

	// TODO clean up this code and move it elsewhere

	def bounds(segmentIndex: Int, gridBounds: GridBounds): Int = {
		val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
		val segmentRows = segmentTransform.segmentRows
		val segmentCols = segmentTransform.segmentCols
		val tileWidth = segmentLayout.tileLayout.tileCols

		val startCol = segmentTransform.indexToCol(0)
		val startRow = segmentTransform.indexToRow(0)

		val endCol =
			if (segmentLayout.isStriped)
				segmentLayout.getSegmentDimensions(segmentIndex)._1
			else
				(startCol + segmentCols) - 1
		
		val endRow =
			if (segmentLayout.isStriped)
				(segmentIndex * segmentRows) + segmentRows
			else
				(startRow + segmentRows) - 1

		val leftCol = math.max(startCol, gridBounds.colMin)
		val rightCol = math.min(endCol, gridBounds.colMax)

		rightCol - leftCol
	}


	def start(segmentIndex: Int, gridBounds: GridBounds): Int = {
		val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
		val segmentRows = segmentTransform.segmentRows
		val segmentCols = segmentTransform.segmentCols
		val tileWidth = segmentLayout.tileLayout.tileCols

		val startCol = segmentTransform.indexToCol(0)
		val startRow = segmentTransform.indexToRow(0)

		val endCol =
			if (segmentLayout.isStriped)
				segmentLayout.getSegmentDimensions(segmentIndex)._1
			else
				(startCol + segmentCols) - 1
		
		val endRow =
			if (segmentLayout.isStriped)
				(segmentIndex * segmentRows) + segmentRows
			else
				(startRow + segmentRows) - 1

		val beginningCol = if (startCol >= gridBounds.colMin) 0 else gridBounds.colMin
		val beginningRow = math.max(startRow, gridBounds.rowMin)

		if (segmentLayout.isStriped)
			((beginningRow - startRow) * cols) + beginningCol
		else
			((beginningRow - startRow) * tileWidth) + beginningCol
	}
	
	def end(segmentIndex: Int, gridBounds: GridBounds): Int = {
		val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
		val segmentRows = segmentTransform.segmentRows
		val segmentCols = segmentTransform.segmentCols
		val tileWidth = segmentLayout.tileLayout.tileCols

		val startCol = segmentTransform.indexToCol(0)
		val startRow = segmentTransform.indexToRow(0)

		val endCol =
			if (segmentLayout.isStriped)
				segmentLayout.getSegmentDimensions(segmentIndex)._1
			else
				(startCol + segmentCols) - 1
		
		val endRow =
			if (segmentLayout.isStriped)
				(segmentIndex * segmentRows) + segmentRows
			else
				(startRow + segmentRows) - 1

		val endingCol =
				if (endCol > gridBounds.colMax)
					gridBounds.colMax - startCol
				else
					endCol - startCol
		
		val endingRow =
			if (endRow > gridBounds.rowMax)
				gridBounds.rowMax - startRow
			else
				endRow - startRow

		val starting = start(segmentIndex, gridBounds)

		if (segmentLayout.isStriped)
			if (math.min(endCol, gridBounds.colMax) != endCol && math.min(endRow, gridBounds.rowMax) != endRow)
				(cols * endingRow) + endingCol
			else
				starting + (cols * endingRow)
		else
			(tileWidth * endingRow) + endingCol
	}

  def withNoData(noDataValue: Option[Double]): UByteGeoTiffTile =
    new UByteGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: UByteCells with NoDataHandling =>
        new UByteGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
