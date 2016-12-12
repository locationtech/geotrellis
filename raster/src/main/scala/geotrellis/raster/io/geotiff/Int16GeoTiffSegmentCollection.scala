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

trait Int16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int16GeoTiffSegment

  val bandType = Int16BandType
  def noDataValue: Option[Short]

  lazy val decompressGeoTiffSegment = noDataValue match {
    case None =>
      (i: Int, bytes: Array[Byte]) => new Int16RawGeoTiffSegment(decompressor.decompress(bytes, i))
    case Some(nd) if nd == Short.MinValue =>
      (i: Int, bytes: Array[Byte]) => new Int16ConstantNoDataGeoTiffSegment(decompressor.decompress(bytes, i))
    case Some(nd) =>
      (i: Int, bytes: Array[Byte]) => new Int16UserDefinedNoDataGeoTiffSegment(decompressor.decompress(bytes, i), nd)
  }
}
