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
    SinglebandGeoTiff(tile, extent, crs, tags, options.copy(storageMethod = storageMethod), overviews)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent, options: Crop.Options): SinglebandGeoTiff = {
    val raster: Raster[Tile] =
      this.raster.crop(subExtent, options)

    SinglebandGeoTiff(raster, subExtent, this.crs, this.tags, this.options, this.overviews)
  }

  def crop(colMax: Int, rowMax: Int): SinglebandGeoTiff =
    crop(0, 0, colMax, rowMax)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): SinglebandGeoTiff = {
    val raster: Raster[Tile] =
      this.raster.crop(colMin, rowMin, colMax, rowMax)

    SinglebandGeoTiff(raster, raster._2, this.crs, this.tags, this.options, this.overviews)
  }

  def crop(subExtent: Extent): SinglebandGeoTiff = crop(subExtent, Crop.Options.DEFAULT)

  def crop(subExtent: Extent, cellSize: CellSize, resampleMethod: ResampleMethod, strategy: OverviewStrategy): SinglebandRaster =
    getClosestOverview(cellSize, strategy)
      .crop(subExtent, Crop.Options(clamp = false))
      .resample(RasterExtent(subExtent, cellSize), resampleMethod, strategy)

  def resample(rasterExtent: RasterExtent, resampleMethod: ResampleMethod, strategy: OverviewStrategy): SinglebandRaster =
    getClosestOverview(cellSize, strategy)
      .raster
      .resample(rasterExtent, resampleMethod)

  def buildOverview(resampleMethod: ResampleMethod, decimationFactor: Int, blockSize: Int): SinglebandGeoTiff = {
    // pad overview with extra cells to keep 1 source pixel = d overview pixels alignment
    // this may cause the overview extent to expand to cover the wider pixels as well
    val padCols: Int = if (tile.cols % decimationFactor == 0) 0 else decimationFactor - tile.cols % decimationFactor
    val padRows: Int = if (tile.rows % decimationFactor == 0) 0 else decimationFactor - tile.rows % decimationFactor
    val overviewRasterExtent = RasterExtent(
      Extent(
        xmin = extent.xmin,
        ymin = extent.ymin - padRows * cellSize.height,
        xmax = extent.xmax + padCols * cellSize.width,
        ymax = extent.ymax),
      cols = math.ceil(tile.cols.toDouble / decimationFactor).toInt,
      rows = math.ceil(tile.rows.toDouble / decimationFactor).toInt
    )

    val segmentLayout: GeoTiffSegmentLayout = GeoTiffSegmentLayout(
      totalCols = overviewRasterExtent.cols,
      totalRows = overviewRasterExtent.rows,
      storageMethod = Tiled(blockSize, blockSize),
      interleaveMethod = PixelInterleave,
      bandType = BandType.forCellType(tile.cellType))

    val segments = for {
      layoutCol <- Iterator.range(0, segmentLayout.tileLayout.layoutCols)
      layoutRow <- Iterator.range(0, segmentLayout.tileLayout.layoutRows)
    } yield {
      val segmentBounds = GridBounds(
        colMin = layoutCol * blockSize,
        rowMin = layoutRow * blockSize,
        colMax = (layoutCol + 1) * blockSize - 1,
        rowMax = (layoutRow + 1) * blockSize - 1)
      val segmentRasterExtent = RasterExtent(
        extent = overviewRasterExtent.extentFor(gridBounds = segmentBounds, clamp = false),
        cols = blockSize,
        rows = blockSize)

      val segmentTile = raster.resample(segmentRasterExtent, resampleMethod).tile

      ((layoutCol, layoutRow), segmentTile)
    }

    val storageMethod = Tiled(blockSize, blockSize)
    val overviewOptions = options.copy(subfileType = Some(ReducedImage), storageMethod = storageMethod)
    val overviewTile = GeoTiffBuilder[Tile].makeTile(
      segments, segmentLayout, cellType, options.compression
    )

    SinglebandGeoTiff(overviewTile, overviewRasterExtent.extent, crs, Tags.empty, overviewOptions)
  }

  def copy(tile: Tile, extent: Extent, crs: CRS, tags: Tags, options: GeoTiffOptions, overviews: List[GeoTiff[Tile]]): SinglebandGeoTiff =
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
  def apply(path: String, decompress: Boolean, streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, decompress, streaming)

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
    GeoTiffReader.readSingleband(path, false, false)

  /** Read a single-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, false, false)

  def streaming(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, false, true)

  def streaming(byteReader: ByteReader): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(byteReader, false, true)
}
