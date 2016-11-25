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

trait UByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UByteGeoTiffSegment

  def noDataValue: Option[Int]
  val bandType = UByteBandType

  lazy val createSegment: Int => UByteGeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new UByteRawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == 0.toShort) =>
      { i: Int => new UByteConstantNoDataCellTypeGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) => // Cast nodata to int in this case so that we can properly compare it to the upcast unsigned byte
      { i: Int => new UByteUserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd.toByte) }
    }
}
