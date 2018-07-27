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
import geotrellis.raster.crop.Crop
import geotrellis.raster.resample.ResampleMethod

case class MultibandGeoTiff(
  tile: MultibandTile,
  extent: Extent,
  crs: CRS,
  tags: Tags,
  options: GeoTiffOptions,
  overviews: List[GeoTiff[MultibandTile]] = Nil
) extends GeoTiff[MultibandTile] {
  val cellType = tile.cellType

  def mapTile(f: MultibandTile => MultibandTile): MultibandGeoTiff =
    MultibandGeoTiff(f(tile), extent, crs, tags, options, overviews)

  def withStorageMethod(storageMethod: StorageMethod): MultibandGeoTiff =
    new MultibandGeoTiff(tile.toArrayTile, extent, crs, tags, options.copy(storageMethod = storageMethod), overviews.map(_.withStorageMethod(storageMethod)))

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffMultibandTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent): MultibandGeoTiff = crop(subExtent, Crop.Options.DEFAULT)

  def crop(subExtent: Extent, options: Crop.Options): MultibandGeoTiff = {
    extent.intersection(subExtent) match {
      case Some(ext) =>
        val raster: Raster[MultibandTile] = this.raster.crop(ext, options)
        MultibandGeoTiff(raster.tile, raster.extent, this.crs, this.tags, this.options, this.overviews)
      case _ => throw GeoAttrsError(s"Extent to crop by ($subExtent) should intersect the imagery extent ($extent).")
    }
  }

  def crop(colMax: Int, rowMax: Int): MultibandGeoTiff =
    crop(0, 0, colMax, rowMax)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): MultibandGeoTiff = {
    val raster: Raster[MultibandTile] =
      this.raster.crop(colMin, rowMin, colMax, rowMax)

    MultibandGeoTiff(raster.tile, raster.extent, this.crs, this.tags, this.options, this.overviews)
  }

  def crop(gridBounds: GridBounds): MultibandGeoTiff =
    crop(gridBounds.colMin, gridBounds.rowMin, gridBounds.colMax, gridBounds.rowMax)

  def crop(subExtent: Extent, cellSize: CellSize, resampleMethod: ResampleMethod, strategy: OverviewStrategy): MultibandRaster =
    getClosestOverview(cellSize, strategy)
      .crop(subExtent, Crop.Options(clamp = false))
      .resample(RasterExtent(subExtent, cellSize), resampleMethod, strategy)

  def crop(windows: Seq[GridBounds]): Iterator[(GridBounds, MultibandTile)] = tile match {
    case geotiffTile: GeoTiffMultibandTile => geotiffTile.crop(windows)
    case arrayTile: MultibandTile => arrayTile.crop(windows)
  }

  def resample(rasterExtent: RasterExtent, resampleMethod: ResampleMethod, strategy: OverviewStrategy): MultibandRaster =
    getClosestOverview(cellSize, strategy)
      .raster
      .resample(rasterExtent, resampleMethod)

  def buildOverview(resampleMethod: ResampleMethod, decimationFactor: Int, blockSize: Int): MultibandGeoTiff = {
    val overviewRasterExtent = RasterExtent(
      extent,
      cols = math.ceil(tile.cols.toDouble / decimationFactor).toInt,
      rows = math.ceil(tile.rows.toDouble / decimationFactor).toInt
    )

    val segmentLayout: GeoTiffSegmentLayout = GeoTiffSegmentLayout(
      totalCols = overviewRasterExtent.cols,
      totalRows = overviewRasterExtent.rows,
      storageMethod = Tiled(blockSize, blockSize),
      interleaveMethod = PixelInterleave,
      bandType = BandType.forCellType(tile.cellType))

    // force ArrayTile to avoid costly compressor thrashing in GeoTiff segments when resample will stride segments
    val arrayTile = tile match {
      case tiffTile: GeoTiffMultibandTile =>
        tiffTile.toArrayTile() // allow GeoTiff tile to read segments in optimal way
      case _ =>
        MultibandTile(tile.bands.map(_.toArrayTile()))
    }

    val segments: Seq[((Int, Int), MultibandTile)] = Raster(arrayTile, extent)
      .resample(overviewRasterExtent, resampleMethod)
      .tile
      .split(segmentLayout.tileLayout)
      .zipWithIndex
      .map { case (tile, index) =>
        val col = index % segmentLayout.tileLayout.layoutCols
        val row = index / segmentLayout.tileLayout.layoutCols
        ((col, row), tile)
      }

    val storageMethod = Tiled(blockSize, blockSize)
    val overviewOptions = options.copy(subfileType = Some(ReducedImage), storageMethod = storageMethod)
    val overviewTile = GeoTiffBuilder[MultibandTile].makeTile(
      segments.toIterator, segmentLayout, cellType, options.compression
    )

    MultibandGeoTiff(overviewTile, extent, crs, Tags.empty, overviewOptions)
  }

  def withOverviews(resampleMethod: ResampleMethod, decimations: List[Int] = Nil, blockSize: Int = GeoTiff.DefaultBlockSize): MultibandGeoTiff = {
    val overviewDecimations: List[Int] =
      if (decimations.isEmpty) {
        GeoTiff.defaultOverviewDecimations(tile.cols, tile.rows, blockSize)
      } else {
        decimations
      }

    if (overviewDecimations.isEmpty) {
      this
    } else {
      // force ArrayTile to avoid costly compressor thrashing in GeoTiff segments when resample will stride segments
      val arrayTile = tile match {
        case tiffTile: GeoTiffMultibandTile =>
          tiffTile.toArrayTile() // allow GeoTiff tile to read segments in optimal way
        case _ =>
          MultibandTile(tile.bands.map(_.toArrayTile()))
      }
      val staged = MultibandGeoTiff(arrayTile, extent, crs, tags, options, Nil)
      val overviews = overviewDecimations.map { (decimationFactor: Int) =>
        staged.buildOverview(resampleMethod, decimationFactor, blockSize)
      }
      MultibandGeoTiff(tile, extent, crs, tags, options, overviews)
    }
  }

  def copy(tile: MultibandTile = tile, extent: Extent = extent, crs: CRS = crs, tags: Tags = tags, options: GeoTiffOptions = options, overviews: List[GeoTiff[MultibandTile]] = overviews): MultibandGeoTiff =
    MultibandGeoTiff(tile, extent, crs, tags, options, overviews)
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
  def apply(bytes: Array[Byte], streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(bytes, streaming)

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
  def apply(path: String, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, streaming)

  def apply(byteReader: ByteReader): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader)

  def apply(byteReader: ByteReader, e: Extent): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, e)

  def apply(byteReader: ByteReader, e: Option[Extent]): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, e)

  def apply(byteReader: ByteReader, streaming: Boolean): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, streaming)

  def streaming(path: String): MultibandGeoTiff =
    GeoTiffReader.readMultiband(path, true)

  def streaming(byteReader: ByteReader): MultibandGeoTiff =
    GeoTiffReader.readMultiband(byteReader, true)

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
