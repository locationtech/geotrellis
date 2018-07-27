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
import spire.syntax.cfor._

import java.nio.ByteBuffer

case class SinglebandGeoTiff(
  tile: Tile,
  extent: Extent,
  crs: CRS,
  tags: Tags,
  options: GeoTiffOptions,
  overviews: List[GeoTiff[Tile]] = Nil
) extends GeoTiff[Tile] {
  val cellType = tile.cellType

  def mapTile(f: Tile => Tile): SinglebandGeoTiff =
    SinglebandGeoTiff(f(tile), extent, crs, tags, options, overviews)

  def withStorageMethod(storageMethod: StorageMethod): SinglebandGeoTiff =
    SinglebandGeoTiff(tile.toArrayTile, extent, crs, tags, options.copy(storageMethod = storageMethod), overviews)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent, options: Crop.Options): SinglebandGeoTiff = {
    extent.intersection(subExtent) match {
      case Some(ext) =>
        val raster: Raster[Tile] = this.raster.crop(ext, options)
        SinglebandGeoTiff(raster.tile, raster.extent, this.crs, this.tags, this.options, this.overviews)
      case _ => throw GeoAttrsError(s"Extent to crop by ($subExtent) should intersect the imagery extent ($extent).")
    }
  }

  def crop(colMax: Int, rowMax: Int): SinglebandGeoTiff =
    crop(0, 0, colMax, rowMax)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): SinglebandGeoTiff = {
    val raster: Raster[Tile] =
      this.raster.crop(colMin, rowMin, colMax, rowMax)

    SinglebandGeoTiff(raster.tile, raster.extent, this.crs, this.tags, this.options, this.overviews)
  }

  def crop(gridBounds: GridBounds): SinglebandGeoTiff =
    crop(gridBounds.colMin, gridBounds.rowMin, gridBounds.colMax, gridBounds.rowMax)

  def crop(subExtent: Extent): SinglebandGeoTiff = crop(subExtent, Crop.Options.DEFAULT)

  def crop(subExtent: Extent, cellSize: CellSize, resampleMethod: ResampleMethod, strategy: OverviewStrategy): SinglebandRaster =
    getClosestOverview(cellSize, strategy)
      .crop(subExtent, Crop.Options(clamp = false))
      .resample(RasterExtent(subExtent, cellSize), resampleMethod, strategy)

  def crop(windows: Seq[GridBounds]): Iterator[(GridBounds, Tile)] = tile match {
    case geotiffTile: GeoTiffTile => geotiffTile.crop(windows)
    case arrayTile: Tile => arrayTile.crop(windows)
  }

  def resample(rasterExtent: RasterExtent, resampleMethod: ResampleMethod, strategy: OverviewStrategy): SinglebandRaster =
    getClosestOverview(cellSize, strategy)
      .raster
      .resample(rasterExtent, resampleMethod)

  def buildOverview(resampleMethod: ResampleMethod, decimationFactor: Int, blockSize: Int): SinglebandGeoTiff = {
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
    val segments: Seq[((Int, Int), Tile)] = Raster(tile.toArrayTile(), extent)
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
    val overviewTile = GeoTiffBuilder[Tile].makeTile(
      segments.toIterator, segmentLayout, cellType, options.compression
    )

    SinglebandGeoTiff(overviewTile, extent, crs, Tags.empty, overviewOptions)
  }

  def withOverviews(resampleMethod: ResampleMethod, decimations: List[Int] = Nil, blockSize: Int = GeoTiff.DefaultBlockSize): SinglebandGeoTiff = {
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
      val arrayTile = tile.toArrayTile()
      val staged = SinglebandGeoTiff(arrayTile, extent, crs, tags, options, Nil)
      val overviews = overviewDecimations.map { (decimationFactor: Int) =>
        staged.buildOverview(resampleMethod, decimationFactor, blockSize)
      }
      SinglebandGeoTiff(tile, extent, crs, tags, options, overviews)
    }
  }

  def copy(tile: Tile = tile, extent: Extent = extent, crs: CRS = crs, tags: Tags = tags, options: GeoTiffOptions = options, overviews: List[GeoTiff[Tile]] = overviews): SinglebandGeoTiff =
    SinglebandGeoTiff(tile, extent, crs, tags, options, overviews)
}

object SinglebandGeoTiff {

  def apply(
    tile: Tile,
    extent: Extent,
    crs: CRS
  ): SinglebandGeoTiff =
    SinglebandGeoTiff(tile, extent, crs, Tags.empty, GeoTiffOptions.DEFAULT)

  /** Read a single-band GeoTIFF file from a byte array.
    */
  def apply(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes)

  /** Read a single-band GeoTIFF file from a byte array.
    */
  def apply(bytes: Array[Byte], streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, streaming)

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
    */
  def apply(path: String, streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, streaming)

  def apply(byteReader: ByteReader): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader)

  def apply(byteReader: ByteReader, e: Extent): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader, e)

  def apply(byteReader: ByteReader, e: Option[Extent]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader, e)

  def streaming(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, true)

  def streaming(byteReader: ByteReader): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader, true)
}
