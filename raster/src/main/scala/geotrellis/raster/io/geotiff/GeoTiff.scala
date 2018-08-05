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

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.proj4.CRS

/**
 * Holds information on how the data is represented, projected, and any user
 * defined tags.
 */
trait GeoTiffData {
  val cellType: CellType

  def imageData: GeoTiffImageData
  def overviews: List[GeoTiffData]
  def extent: Extent
  def crs: CRS
  def tags: Tags
  def options: GeoTiffOptions

  def pixelSampleType: Option[PixelSampleType] =
    tags.headTags.get(Tags.AREA_OR_POINT).flatMap { aop =>
      aop match {
        case "AREA" => Some(PixelIsArea)
        case "POINT" => Some(PixelIsPoint)
        case _ => None
      }
    }
}

/**
 * Base trait of GeoTiff. Takes a tile that is of a type equal to or a subtype
 * of CellGrid
 */
trait GeoTiff[T <: CellGrid] extends GeoTiffData {
  def tile: T

  def cols: Int = tile.cols
  def rows: Int = tile.rows
  def projectedExtent: ProjectedExtent = ProjectedExtent(extent, crs)
  def projectedRaster: ProjectedRaster[T] = ProjectedRaster(tile, extent, crs)
  def raster: Raster[T] = Raster(tile, extent)
  def rasterExtent: RasterExtent = RasterExtent(extent, tile.cols, tile.rows)
  def cellSize: CellSize = rasterExtent.cellSize
  def bandCount: Int = tile match {
    case t: MultibandTile => t.bandCount
    case _ => 1
  }

  def mapTile(f: T => T): GeoTiff[T]

  def withStorageMethod(storageMethod: StorageMethod): GeoTiff[T]

  def write(path: String, optimizedOrder: Boolean = false): Unit =
    GeoTiffWriter.write(this, path, optimizedOrder)

  def toByteArray: Array[Byte] =
    GeoTiffWriter.write(this, false)

  def toCloudOptimizedByteArray: Array[Byte] =
    GeoTiffWriter.write(this, true)

  def overviews: List[GeoTiff[T]]
  def getOverviewsCount: Int = overviews.length
  def getOverview(idx: Int): GeoTiff[T] = if(idx < 0) this else overviews(idx)
  def buildOverview(resampleMethod: ResampleMethod, decimationFactor: Int, blockSize: Int = GeoTiff.DefaultBlockSize): GeoTiff[T]
  def withOverviews(resampleMethod: ResampleMethod, decimations: List[Int] = Nil, blockSize: Int = GeoTiff.DefaultBlockSize): GeoTiff[T]
  def withOverviews(overviews: Seq[GeoTiff[T]]): GeoTiff[T] = copy(overviews = overviews.toList)

  /** Chooses the best matching overviews and makes resample */
  def resample(rasterExtent: RasterExtent, resampleMethod: ResampleMethod, strategy: OverviewStrategy): Raster[T]

  /** Chooses the best matching overviews and makes resample & crop */
  def crop(subExtent: Extent, cellSize: CellSize, resampleMethod: ResampleMethod, strategy: OverviewStrategy): Raster[T]
  def crop(subExtent: Extent, cellSize: CellSize): Raster[T] = crop(subExtent, cellSize, NearestNeighbor, AutoHigherResolution)
  def crop(rasterExtent: RasterExtent): Raster[T] = crop(rasterExtent.extent, rasterExtent.cellSize)

  def crop(subExtent: Extent, options: Crop.Options): GeoTiff[T]
  def crop(subExtent: Extent): GeoTiff[T]
  def crop(colMax: Int, rowMax: Int): GeoTiff[T]
  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): GeoTiff[T]
  def crop(gridBounds: GridBounds): GeoTiff[T]
  def crop(windows: Seq[GridBounds]): Iterator[(GridBounds, T)]

  /** Return the best matching overview to the given cellSize, returns "this" if no overviews available. */
  private[geotrellis] def getClosestOverview(cellSize: CellSize, strategy: OverviewStrategy): GeoTiff[T] = {
    overviews match {
      case Nil => this
      case list =>
        strategy match {
          case AutoHigherResolution =>
            (this :: list) // overviews can have erased extent information
              .map { v => (v.cellSize.resolution - cellSize.resolution) -> v }
              .filter(_._1 >= 0)
              .sortBy(_._1)
              .map(_._2)
              .headOption
              .getOrElse(this)
          case Auto(n) =>
            list
              .sortBy(v => math.abs(v.cellSize.resolution - cellSize.resolution))
              .lift(n)
              .getOrElse(this) // n can be out of bounds,
          // makes only overview lookup as overview position is important
          case Base => this
        }
    }
  }

  def copy(
    tile: T = this.tile,
    extent: Extent = this.extent,
    crs: CRS = this.crs,
    tags: Tags = this.tags,
    options: GeoTiffOptions = this.options,
    overviews: List[GeoTiff[T]] = this.overviews
  ): GeoTiff[T]
}

/**
 * Companion object to GeoTiff
 */
object GeoTiff {
  val DefaultBlockSize = 128 // match GDAL default

  def readMultiband(path: String): MultibandGeoTiff =
    MultibandGeoTiff(path)

  def readSingleband(path: String): SinglebandGeoTiff =
    SinglebandGeoTiff(path)

  def apply(path: String): Either[SinglebandGeoTiff, MultibandGeoTiff] = {
    val multiband = MultibandGeoTiff(path)
    if (multiband.tile.bandCount == 1) {
      Left(new SinglebandGeoTiff(tile = multiband.tile.band(0),
        multiband.extent, multiband.crs, multiband.tags, multiband.options))
    } else {
      Right(multiband)
    }
  }

  private[raster]
  def defaultOverviewDecimations(cols: Int, rows: Int, blockSize: Int): List[Int] = {
      val overviewLevels: Int = {
        val pixels = math.max(cols, rows).toDouble
        val blocks = pixels / blockSize
        math.ceil(math.log(blocks) / math.log(2)).toInt
      }

      (0 until overviewLevels).map{ l => math.pow(2, l + 1).toInt }.toList
  }

  def apply(tile: Tile, extent: Extent, crs: CRS): SinglebandGeoTiff =
    SinglebandGeoTiff(tile, extent, crs)

  def apply(raster: SinglebandRaster, crs: CRS): SinglebandGeoTiff =
    apply(raster.tile, raster.extent, crs)

  def apply(tile: MultibandTile, extent: Extent, crs: CRS): MultibandGeoTiff =
    MultibandGeoTiff(tile, extent, crs)

  def apply(raster: MultibandRaster, crs: CRS): MultibandGeoTiff =
    apply(raster.tile, raster.extent, crs)

  def apply(projectedRaster: ProjectedRaster[Tile]): SinglebandGeoTiff =
    apply(projectedRaster.raster, projectedRaster.crs)

  def apply(projectedRaster: ProjectedRaster[MultibandTile])(implicit d: DummyImplicit): MultibandGeoTiff =
    apply(projectedRaster.raster, projectedRaster.crs)
}
