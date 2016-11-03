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

import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.tags.codes.ColorSpace

/**
  * This case class holds information about how the data is stored in
  * a [[GeoTiff]]. If no values are given directly, then the defaults
  * are used.
  */
case class GeoTiffOptions(
  storageMethod: StorageMethod = GeoTiffOptions.DEFAULT.storageMethod,
  compression: Compression = GeoTiffOptions.DEFAULT.compression,
  colorSpace: Int = GeoTiffOptions.DEFAULT.colorSpace
)

/**
  * The companion object to [[GeoTiffOptions]]
  */
object GeoTiffOptions {
  val DEFAULT = GeoTiffOptions(Striped, NoCompression, ColorSpace.BlackIsZero)

  /**
    * Creates a new instance of [[GeoTiffOptions]] with the given
    * StorageMethod and the default compression value
    */
  def apply(storageMethod: StorageMethod): GeoTiffOptions =
    GeoTiffOptions(storageMethod, DEFAULT.compression, DEFAULT.colorSpace)

  /**
    * Creates a new instance of [[GeoTiffOptions]] with the given
    * Compression and the default [[StorageMethod]] value
    */
  def apply(compression: Compression): GeoTiffOptions =
    GeoTiffOptions(DEFAULT.storageMethod, compression, DEFAULT.colorSpace)
}
