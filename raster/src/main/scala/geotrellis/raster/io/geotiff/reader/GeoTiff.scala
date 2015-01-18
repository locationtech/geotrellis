/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff.reader

/**
  * Represents a GeoTiff file.
  *
  * This class provides a basic interface for the GeoTiff data that is most
  * commonly used. The data includes the tile, the extends, the coordinate
  * system and the metadata. Also multiple bands are supported.
  *
  * If access to other tags are required, this class also exposes the
  * image directory class for accessing all parsed Tiff tags and GeoTiff
  * directory keys.
  */
case class GeoTiff(imageDirectory: ImageDirectory) {

  val (tile, extent, crs) = imageDirectory.toRaster

  val toRaster = (tile, extent, crs)

  val bands = imageDirectory.bands

  val hasBands = bands.size > 1

  val metadata = imageDirectory.metadata

  val bandsMetadata = imageDirectory.bandsMetadata

}
