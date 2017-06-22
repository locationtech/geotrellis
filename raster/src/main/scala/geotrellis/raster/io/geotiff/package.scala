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

package geotrellis.raster.io

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

package object geotiff {
  val DefaultCompression = DeflateCompression
  val Deflate = DeflateCompression

  implicit class GeoTiffTileMethods(val tile: Tile) extends AnyRef {
    def toGeoTiffTile(): GeoTiffTile =
      GeoTiffTile(tile)

    def toGeoTiffTile(options: GeoTiffOptions): GeoTiffTile =
      GeoTiffTile(tile, options)

    def toGeoTiffTile(compression: Compression): GeoTiffTile =
      GeoTiffTile(tile, GeoTiffOptions(compression))

    def toGeoTiffTile(layout: StorageMethod): GeoTiffTile =
      GeoTiffTile(tile, GeoTiffOptions(layout))
  }

  implicit class GeoTiffMultibandTileMethods(val tile: MultibandTile) extends AnyRef {
    def toGeoTiffTile(): GeoTiffMultibandTile =
      GeoTiffMultibandTile(tile)

    def toGeoTiffTile(options: GeoTiffOptions): GeoTiffMultibandTile =
      GeoTiffMultibandTile(tile, options)

    def toGeoTiffTile(compression: Compression): GeoTiffMultibandTile =
      GeoTiffMultibandTile(tile, GeoTiffOptions(compression))

    def toGeoTiffTile(layout: StorageMethod): GeoTiffMultibandTile =
      GeoTiffMultibandTile(tile, GeoTiffOptions(layout))
  }
}
