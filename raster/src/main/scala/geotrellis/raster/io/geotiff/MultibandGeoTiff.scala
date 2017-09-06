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
import geotrellis.raster.resample.ResampleMethod

case class MultibandGeoTiff(
  tile: MultibandTile,
  extent: Extent,
  crs: CRS,
  tags: Tags,
  options: GeoTiffOptions,
  overviews: List[MultibandGeoTiff] = Nil
) extends GeoTiff[MultibandTile] {
  val cellType = tile.cellType

  def mapTile(f: MultibandTile => MultibandTile): MultibandGeoTiff =
    MultibandGeoTiff(f(tile), extent, crs, tags, options, overviews)

  def withStorageMethod(storageMethod: StorageMethod): MultibandGeoTiff =
    new MultibandGeoTiff(tile, extent, crs, tags, options.copy(storageMethod = storageMethod), overviews.map(_.withStorageMethod(storageMethod)))

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffMultibandTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent): MultibandGeoTiff = {
    val raster: Raster[MultibandTile] =
      this.raster.crop(subExtent)

    MultibandGeoTiff(raster, subExtent, this.crs, this.tags, this.options, this.overviews)
  }

  def crop(colMax: Int, rowMax: Int): MultibandGeoTiff =
    crop(0, 0, colMax, rowMax)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): MultibandGeoTiff = {
    val raster: Raster[MultibandTile] =
      this.raster.crop(colMin, rowMin, colMax, rowMax)

    MultibandGeoTiff(raster, raster._2, this.crs, this.tags, this.options, this.overviews)
  }

  def crop(subExtent: Extent, cellSize: CellSize, resampleMethod: ResampleMethod, strategy: OverviewStrategy): MultibandRaster =
    resample(RasterExtent(subExtent, cellSize), resampleMethod, strategy).crop(subExtent)

  def resample(rasterExtent: RasterExtent, resampleMethod: ResampleMethod, strategy: OverviewStrategy): MultibandRaster =
    getClosestOverview(cellSize, strategy)
      .raster
      .resample(rasterExtent, resampleMethod)
}

object MultibandGeoTiff {
  /** Read a multi-band GeoTIFF file from a byte array.
    * GeoTIFF will be fully uncompressed and held in memory.
    */
  def apply(bytes: Array[Byte]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes)

  /** Read a multi-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully uncompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, decompress, streaming)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path)

  def apply(path: String, e: Extent): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, e)

  def apply(path: String, e: Option[Extent]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, e)

  /** Read a multi-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, decompress, streaming)

  def apply(byteReader: ByteReader): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader)

  def apply(byteReader: ByteReader, e: Extent): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, e)

  def apply(byteReader: ByteReader, e: Option[Extent]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, e)

  def apply(byteReader: ByteReader, decompress: Boolean, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, decompress, streaming)

  /** Read a multi-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, false, false)

  /** Read a multi-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, false, false)

  def streaming(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, false, true)

  def streaming(byteReader: ByteReader): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, false, true)

  def apply(
    tile: MultibandTile,
    extent: Extent,
    crs: CRS
  ): MultibandGeoTiff =
    apply(tile, extent, crs, Tags.empty)

  def apply(
    tile: MultibandTile,
    extent: Extent,
    crs: CRS,
    tags: Tags
  ): MultibandGeoTiff =
    apply(tile, extent, crs, tags, GeoTiffOptions.DEFAULT)

  def apply(
    tile: MultibandTile,
    extent: Extent,
    crs: CRS,
    options: GeoTiffOptions
  ): MultibandGeoTiff =
    apply(tile, extent, crs, Tags.empty, options)

  def apply(
    raster: Raster[MultibandTile],
    crs: CRS
  ): MultibandGeoTiff =
    apply(raster.tile, raster.extent, crs, Tags.empty)

  def apply(
    raster: Raster[MultibandTile],
    crs: CRS,
    tags: Tags
  ): MultibandGeoTiff =
    apply(raster.tile, raster.extent, crs, tags, GeoTiffOptions.DEFAULT)

  def apply(
    raster: Raster[MultibandTile],
    crs: CRS,
    options: GeoTiffOptions
  ): MultibandGeoTiff =
    apply(raster.tile, raster.extent, crs, Tags.empty, options)
}
