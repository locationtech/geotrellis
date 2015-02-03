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

import geotrellis.raster._

import geotrellis.vector.Extent

import geotrellis.proj4.CRS

import monocle.syntax._

case class GeoTiffMetaData(rasterExtent:RasterExtent, crs: CRS, cellType: CellType) {
  def extent: Extent = rasterExtent.extent
  def cols: Int = rasterExtent.cols
  def rows: Int = rasterExtent.rows
}

/**
  * Represents a GeoTiff. Has a sequence of bands and the metadata.
  */
case class GeoTiff(
  metaData: GeoTiffMetaData,
  bands: Seq[GeoTiffBand],
  tags: Map[String, String],
  imageDirectory: ImageDirectory) {

  lazy val firstBand: GeoTiffBand = bands.head

  lazy val colorMap: Seq[(Short, Short, Short)] = (imageDirectory &|->
    ImageDirectory._basicTags ^|->
    BasicTags._colorMap get)

}

/**
  * Represents a band in a GeoTiff. Contains a tile, the extent and the crs for
  * the band. Also holds the optional metadata for each band.
  */
case class GeoTiffBand(tile: Tile, extent: Extent, crs: CRS, tags: Map[String, String])
