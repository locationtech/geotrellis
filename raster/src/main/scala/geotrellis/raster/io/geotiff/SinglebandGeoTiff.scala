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

import geotrellis.util.ByteReader
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

case class SinglebandGeoTiff(
  tile: Tile,
  extent: Extent,
  crs: CRS,
  tags: Tags,
  options: GeoTiffOptions
) extends GeoTiff[Tile] {
  val cellType = tile.cellType

  def mapTile(f: Tile => Tile): SinglebandGeoTiff =
    SinglebandGeoTiff(f(tile), extent, crs, tags, options)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent): SinglebandGeoTiff = {
    val raster: Raster[Tile] =
      this.raster.crop(subExtent)

    SinglebandGeoTiff(raster, subExtent, this.crs)
  }

  def crop(colMax: Int, rowMax: Int): SinglebandGeoTiff =
    crop(0, 0, colMax, rowMax)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): SinglebandGeoTiff = {
    val raster: Raster[Tile] =
      this.raster.crop(colMin, rowMin, colMax, rowMax)

    SinglebandGeoTiff(raster, raster._2, this.crs)
  }
}

object SinglebandGeoTiff {

  def apply(
    tile: Tile,
    extent: Extent,
    crs: CRS
  ): SinglebandGeoTiff =
    new SinglebandGeoTiff(tile, extent, crs, Tags.empty, GeoTiffOptions.DEFAULT)

  /** Read a single-band GeoTIFF file from a byte array.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes)

  /** Read a single-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean, streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, decompress, streaming)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path)

  def apply(path: String, e: Extent): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, e)

  def apply(path: String, e: Option[Extent]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, e)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean, streaming: Boolean, extent: Option[Extent]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, decompress, streaming, extent)

  def apply(byteReader: ByteReader): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader)

  def apply(byteReader: ByteReader, e: Extent): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader, e)

  def apply(byteReader: ByteReader, e: Option[Extent]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader, e)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, false, false, None)

  /** Read a single-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, false, false, None)

  def streaming(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, false, true, None)

  def streaming(byteReader: ByteReader): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader, false, true, None)
}
