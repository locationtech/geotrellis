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

trait UInt16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UInt16GeoTiffSegment

  val bandType = UInt16BandType
  val noDataValue: Option[Int]

  lazy val createSegment: Int => UInt16GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new UInt16RawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if (nd == 0) =>
      { i: Int => new UInt16ConstantNoDataGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) => // Cast nodata to int in this case so that we can properly compare it to the upcast unsigned byte
      { i: Int => new UInt16UserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd.toShort) }
    }
}
