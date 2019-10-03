/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.geotiff

import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.util.RangeReader

class GeoTiffRasterSource(
  val dataPath: GeoTiffPath,
  private[raster] val targetCellType: Option[TargetCellType] = None,
  @transient private[raster] val baseTiff: Option[MultibandGeoTiff] = None
) extends RasterSource {
  def name: GeoTiffPath = dataPath

  @transient lazy val tiff: MultibandGeoTiff =
    Option(baseTiff)
      .flatten
      .getOrElse(GeoTiffReader.readMultiband(
        RangeReader(dataPath.value),
        streaming = true, withOverviews = true,
        RangeReader.validated(dataPath.externalOverviews)
      ))

  def bandCount: Int = tiff.bandCount
  def cellType: CellType = dstCellType.getOrElse(tiff.cellType)
  def tags: Tags = tiff.tags
  def metadata: GeoTiffMetadata = GeoTiffMetadata(name, crs, bandCount, cellType, gridExtent, resolutions, tags)

  /** Returns the GeoTiff head tags. */
  def attributes: Map[String, String] = tags.headTags
  /** Returns the GeoTiff per band tags. */
  def attributesForBand(band: Int): Map[String, String] = tags.bandTags.lift(band).getOrElse(Map.empty)

  def crs: CRS = tiff.crs

  lazy val gridExtent: GridExtent[Long] = tiff.rasterExtent.toGridType[Long]
  lazy val resolutions: List[CellSize] = cellSize :: tiff.overviews.map(_.cellSize)

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget = DefaultTarget, method: ResampleMethod = NearestNeighbor, strategy: OverviewStrategy = AutoHigherResolution): GeoTiffReprojectRasterSource =
    GeoTiffReprojectRasterSource(dataPath, targetCRS, resampleTarget, method, strategy, targetCellType = targetCellType, baseTiff = Some(tiff))

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): GeoTiffResampleRasterSource =
    GeoTiffResampleRasterSource(dataPath, resampleTarget, method, strategy, targetCellType, Some(tiff))

  def convert(targetCellType: TargetCellType): GeoTiffRasterSource =
    GeoTiffRasterSource(dataPath, Some(targetCellType), Some(tiff))

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val bounds = gridExtent.gridBoundsFor(extent, clamp = false).toGridType[Int]
    val geoTiffTile = tiff.tile.asInstanceOf[GeoTiffMultibandTile]
    val it = geoTiffTile.crop(List(bounds), bands.toArray).map { case (gb, tile) =>
      // TODO: shouldn't GridExtent give me Extent for types other than N ?
      Raster(tile, gridExtent.extentFor(gb.toGridType[Long], clamp = false))
    }

    // We want to use this tiff in different `RasterSource`s, so we
    // need to lock it in order to garuntee the state of tiff when
    // it's being accessed by a thread.
    tiff.synchronized { if (it.hasNext) Some(convertRaster(it.next)) else None }
  }

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val it = readBounds(List(bounds), bands)

    tiff.synchronized { if (it.hasNext) Some(it.next) else None }
  }

  override def readExtents(extents: Traversable[Extent], bands: Seq[Int]): Iterator[Raster[MultibandTile]] = {
    val bounds = extents.map(gridExtent.gridBoundsFor(_, clamp = true))

    readBounds(bounds, bands)
  }

  override def readBounds(bounds: Traversable[GridBounds[Long]], bands: Seq[Int]): Iterator[Raster[MultibandTile]] = {
    val geoTiffTile = tiff.tile.asInstanceOf[GeoTiffMultibandTile]
    val intersectingBounds: Seq[GridBounds[Int]] =
      bounds.flatMap(_.intersection(this.dimensions)).toSeq.map(_.toGridType[Int])

    geoTiffTile.crop(intersectingBounds, bands.toArray).map { case (gb, tile) =>
      convertRaster(Raster(tile, gridExtent.extentFor(gb.toGridType[Long], clamp = true)))
    }
  }
}

object GeoTiffRasterSource {
  def apply(dataPath: GeoTiffPath, targetCellType: Option[TargetCellType] = None, baseTiff: Option[MultibandGeoTiff] = None): GeoTiffRasterSource =
    new GeoTiffRasterSource(dataPath, targetCellType, baseTiff)
}
