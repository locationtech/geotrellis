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

class Float32GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: FloatCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with Float32GeoTiffSegmentCollection {

  val noDataValue: Option[Float] = cellType match {
    case FloatCellType => None
    case FloatConstantNoDataCellType => Some(Float.NaN)
    case FloatUserDefinedNoDataCellType(nd) => Some(nd)
  }

  /**
   * Reads the data out of a [[GeoTiffTile]] and create a
   * FloatArrayTile.
   *
   * @param  CroppedGeoTiff  The [[WindowedGeoTiff]] of the file
   * @return                 A [[FloatArrayTile]]
   */
  def mutable: MutableArrayTile = crop(gridBounds)

  /**
   * Reads a windowed area out of a [[GeoTiffTile]] and create a
   * FloatArrayTile.
   *
   * @param  CroppedGeoTiff  The [[WindowedGeoTiff]] of the file
   * @return                 A [[FloatArrayTile]] that conatins data from the windowed area
   */
  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val bytesPerPixel: Int = FloatConstantNoDataCellType.bytes
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

    FloatArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }


  def withNoData(noDataValue: Option[Double]): Float32GeoTiffTile =
    new Float32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: FloatCells with NoDataHandling =>
        new Float32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
