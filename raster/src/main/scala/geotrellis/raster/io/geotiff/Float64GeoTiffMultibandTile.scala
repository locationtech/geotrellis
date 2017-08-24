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

class Float64GeoTiffMultibandTile(
  compressedBytes: SegmentBytes,
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  val cellType: DoubleCells with NoDataHandling,
  overviews: List[Float64GeoTiffMultibandTile] = Nil
) extends GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, overviews)
    with Float64GeoTiffSegmentCollection {

  val noDataValue: Option[Double] = cellType match {
    case DoubleCellType => None
    case DoubleConstantNoDataCellType => Some(Double.NaN)
    case DoubleUserDefinedNoDataCellType(nd) => Some(nd)
  }

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner(bandCount) {
      private val arr = Array.ofDim[Double](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = i2d(v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = v
      }

      def getBytes(): Array[Byte] = {
        val result = new Array[Byte](targetSize * DoubleConstantNoDataCellType.bytes)
        val bytebuff = ByteBuffer.wrap(result)
        bytebuff.asDoubleBuffer.put(arr)
        result
      }
    }

  def withNoData(noDataValue: Option[Double]): Float64GeoTiffMultibandTile =
    new Float64GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, cellType.withNoData(noDataValue), overviews.map(_.withNoData(noDataValue)))

  def interpretAs(newCellType: CellType): GeoTiffMultibandTile = {
    newCellType match {
      case dt: DoubleCells with NoDataHandling =>
        new Float64GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, dt, overviews.map(_.interpretAs(newCellType)).collect { case gt: Float64GeoTiffMultibandTile => gt })
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }

}
