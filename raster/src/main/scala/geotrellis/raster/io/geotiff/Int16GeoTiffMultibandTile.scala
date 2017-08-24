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

import java.nio.ByteBuffer

class Int16GeoTiffMultibandTile(
  compressedBytes: SegmentBytes,
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  val cellType: ShortCells with NoDataHandling,
  overviews: List[Int16GeoTiffMultibandTile] = Nil
) extends GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, overviews)
    with Int16GeoTiffSegmentCollection {

  val noDataValue: Option[Short] = cellType match {
    case ShortCellType => None
    case ShortConstantNoDataCellType => Some(Short.MinValue)
    case ShortUserDefinedNoDataCellType(nd) => Some(nd)
  }

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner(bandCount) {
      private val arr = Array.ofDim[Short](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = i2s(v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = d2s(v)
      }

      def getBytes(): Array[Byte] = {
        val result = new Array[Byte](targetSize * ShortConstantNoDataCellType.bytes)
        val bytebuff = ByteBuffer.wrap(result)
        bytebuff.asShortBuffer.put(arr)
        result
      }
    }

  def withNoData(noDataValue: Option[Double]): Int16GeoTiffMultibandTile =
    new Int16GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, cellType.withNoData(noDataValue), overviews.map(_.withNoData(noDataValue)))

  def interpretAs(newCellType: CellType): GeoTiffMultibandTile = {
    newCellType match {
      case dt: ShortCells with NoDataHandling =>
        new Int16GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, dt, overviews.map(_.interpretAs(newCellType)).collect { case gt: Int16GeoTiffMultibandTile => gt })
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
