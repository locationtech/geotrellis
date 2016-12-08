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

class Int32GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: IntCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with Int32GeoTiffSegmentCollection {

  val noDataValue: Option[Int] = cellType match {
    case IntCellType => None
    case IntConstantNoDataCellType => Some(Int.MinValue)
    case IntUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = crop(gridBounds)

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val bytesPerPixel: Int = IntConstantNoDataCellType.bytes
    val arr: Array[Byte] =
      if (segmentLayout.isStriped)
        readStrippedSegmentBytes(segmentBytes,
          segmentLayout,
          bytesPerPixel,
          gridBounds)
      else
        readTiledSegmentBytes(segmentBytes,
          segmentLayout,
          bytesPerPixel,
          gridBounds)

    IntArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }

  def withNoData(noDataValue: Option[Double]): Int32GeoTiffTile =
    new Int32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: IntCells with NoDataHandling =>
        new Int32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
