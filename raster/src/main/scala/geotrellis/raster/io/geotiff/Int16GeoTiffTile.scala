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

class Int16GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: ShortCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with Int16GeoTiffSegmentCollection {

  val noDataValue: Option[Short] = cellType match {
    case ShortCellType => None
    case ShortConstantNoDataCellType => Some(Short.MinValue)
    case ShortUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Short](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment =
        getSegment(segmentIndex)

      val segmentTransform =
        if (segmentLayout.isStriped)
          StripedSegmentTransform(segmentIndex, segmentLayout)
        else
          TiledSegmentTransform(segmentIndex, segmentLayout)

      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          val data = segment.get(i)
          arr(row * cols + col) = data
        }
      }
    }
    ShortArrayTile(arr, cols, rows, cellType)
  }

  def withNoData(noDataValue: Option[Double]): Int16GeoTiffTile =
    new Int16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: ShortCells with NoDataHandling =>
        new Int16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
