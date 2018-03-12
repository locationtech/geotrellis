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
    new MultibandGeoTiff(tile, extent, crs, tags, options.copy(storageMethod = storageMethod), overviews.map(_.withStorageMethod(storageMethod)))

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffMultibandTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent): MultibandGeoTiff = crop(subExtent, Crop.Options.DEFAULT)

  def crop(subExtent: Extent, options: Crop.Options): MultibandGeoTiff = {
    val raster: Raster[MultibandTile] =
      this.raster.crop(subExtent, options)

    MultibandGeoTiff(raster, subExtent, this.crs, this.tags, this.options, this.overviews)
  }

  def crop(colMax: Int, rowMax: Int): MultibandGeoTiff =
    crop(0, 0, colMax, rowMax)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): MultibandGeoTiff = {
    val raster: Raster[MultibandTile] =
      this.raster.crop(colMin, rowMin, colMax, rowMax)

    MultibandGeoTiff(raster, raster._2, this.crs, this.tags, this.options, this.overviews)
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
    // pad overview with extra cells to keep 1 source pixel = d overview pixels alignment
    // this may cause the overview extent to expand to cover the wider pixels as well
    // val padCols: Int = if (tile.cols % decimationFactor == 0) 0 else decimationFactor - tile.cols % decimationFactor
    // val padRows: Int = if (tile.rows % decimationFactor == 0) 0 else decimationFactor - tile.rows % decimationFactor
    val padCols: Int = decimationFactor
    val padRows: Int = decimationFactor
    val overviewRasterExtent = RasterExtent(
      Extent(
        xmin = extent.xmin,
        ymin = extent.ymin - padRows * cellSize.height,
        xmax = extent.xmax + padCols * cellSize.width,
        ymax = extent.ymax
      ),
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
    val overviewTile = GeoTiffBuilder[MultibandTile].makeTile(
      segments, segmentLayout, cellType, options.compression
    )

    MultibandGeoTiff(overviewTile, overviewRasterExtent.extent, crs, Tags.empty, overviewOptions)
  }

  def copy(tile: MultibandTile, extent: Extent, crs: CRS, tags: Tags, options: GeoTiffOptions, overviews: List[GeoTiff[MultibandTile]]): MultibandGeoTiff =
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
