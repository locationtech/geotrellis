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

trait Float32GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Float32GeoTiffSegment

  val bandType = Float32BandType
  val noDataValue: Option[Float]

  lazy val createSegment: Int => Float32GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new Float32RawGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) if java.lang.Float.isNaN(nd) =>
      { i: Int => new Float32ConstantNoDataGeoTiffSegment(getDecompressedBytes(i)) }
    case Some(nd) =>
      { i: Int => new Float32UserDefinedNoDataGeoTiffSegment(getDecompressedBytes(i), nd) }
  }
}
