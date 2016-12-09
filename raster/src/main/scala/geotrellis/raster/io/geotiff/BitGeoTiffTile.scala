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

  override def crop(gridBounds: GridBounds): MutableArrayTile = {
    val result = BitArrayTile.empty(gridBounds.width, gridBounds.height)

    val colMin = gridBounds.colMin
    val rowMin = gridBounds.rowMin
    val tileCols = segmentLayout.tileLayout.tileCols
    val tileRows = segmentLayout.tileLayout.tileRows

    cfor(0)(_ < segmentCount, _ + 1) { i =>
      val segmentTransform =
        if (segmentLayout.isStriped)
          StripedSegmentTransform(i, segmentLayout)
        else
          TiledSegmentTransform(i, segmentLayout)

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
