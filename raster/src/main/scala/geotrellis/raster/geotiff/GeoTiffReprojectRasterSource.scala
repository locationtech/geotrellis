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

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.proj4._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, MultibandGeoTiff, OverviewStrategy, Tags}
import geotrellis.util.RangeReader

class GeoTiffReprojectRasterSource(
  val dataPath: GeoTiffPath,
  val crs: CRS,
  val resampleTarget: ResampleTarget = DefaultTarget,
  val resampleMethod: ResampleMethod = ResampleMethod.DEFAULT,
  val strategy: OverviewStrategy = OverviewStrategy.DEFAULT,
  val errorThreshold: Double = 0.125,
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

  protected lazy val baseCRS: CRS = tiff.crs
  protected lazy val baseGridExtent: GridExtent[Long] = tiff.rasterExtent.toGridType[Long]

  // TODO: remove transient notation with Proj4 1.1 release
  @transient protected lazy val transform = Transform(baseCRS, crs)
  @transient protected lazy val backTransform = Transform(crs, baseCRS)

  override lazy val gridExtent: GridExtent[Long] = {
    lazy val reprojectedRasterExtent =
      ReprojectRasterExtent(
        baseGridExtent,
        transform,
        Reproject.Options.DEFAULT.copy(method = resampleMethod, errorThreshold = errorThreshold)
      )

    resampleTarget(reprojectedRasterExtent)
  }

  lazy val resolutions: List[CellSize] =
    ReprojectRasterExtent(tiff.rasterExtent, transform, Reproject.Options.DEFAULT).cellSize ::
      tiff.overviews.map(ovr => ReprojectRasterExtent(ovr.rasterExtent, transform, Reproject.Options.DEFAULT).cellSize)

  @transient private[raster] lazy val closestTiffOverview: GeoTiff[MultibandTile] = {
    resampleTarget match {
      case DefaultTarget => tiff.getClosestOverview(baseGridExtent.cellSize, strategy)
      case _ =>
        // we're asked to match specific target resolution, estimate what resolution we need in source to sample it
        val estimatedSource = ReprojectRasterExtent(gridExtent, backTransform)
        tiff.getClosestOverview(estimatedSource.cellSize, strategy)
    }
  }

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val bounds = gridExtent.gridBoundsFor(extent, clamp = false)

    read(bounds, bands)
  }

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val it = readBounds(List(bounds), bands)

    tiff.synchronized { if (it.hasNext) Some(it.next()) else None }
  }

  override def readExtents(extents: Traversable[Extent], bands: Seq[Int]): Iterator[Raster[MultibandTile]] = {
    val bounds = extents.map(gridExtent.gridBoundsFor(_))

    readBounds(bounds, bands)
  }

  override def readBounds(bounds: Traversable[GridBounds[Long]], bands: Seq[Int]): Iterator[Raster[MultibandTile]] = {
    val geoTiffTile = closestTiffOverview.tile.asInstanceOf[GeoTiffMultibandTile]
    val intersectingWindows = { for {
      queryPixelBounds <- bounds
      targetPixelBounds <- queryPixelBounds.intersection(this.dimensions)
    } yield {
      val targetExtent = gridExtent.extentFor(targetPixelBounds)
      val targetRasterExtent = RasterExtent(
        extent = targetExtent,
        cols = targetPixelBounds.width.toInt,
        rows = targetPixelBounds.height.toInt
      )

      // buffer the targetExtent to read a buffered area from the source tiff
      // so the resample would behave properly on borders
      val bufferedTargetExtent = targetExtent.buffer(cellSize.width, cellSize.height)

      // A tmp workaround for https://github.com/locationtech/proj4j/pull/29
      // Stacktrace details: https://github.com/geotrellis/geotrellis-contrib/pull/206#pullrequestreview-260115791
      val sourceExtent = Proj4Transform.synchronized(bufferedTargetExtent.reprojectAsPolygon(backTransform, 0.001).getEnvelopeInternal)
      val sourcePixelBounds = closestTiffOverview.rasterExtent.gridBoundsFor(sourceExtent)
      (sourcePixelBounds, targetRasterExtent)
    }}.toMap

    geoTiffTile.crop(intersectingWindows.keys.toSeq, bands.toArray).map { case (sourcePixelBounds, tile) =>
      val targetRasterExtent = intersectingWindows(sourcePixelBounds)
      val sourceRaster = Raster(tile, closestTiffOverview.rasterExtent.extentFor(sourcePixelBounds))
      val rr = implicitly[RasterRegionReproject[MultibandTile]]
      rr.regionReproject(
        sourceRaster,
        baseCRS,
        crs,
        targetRasterExtent,
        targetRasterExtent.extent.toPolygon(),
        resampleMethod,
        errorThreshold
      )
    }.map { convertRaster }
  }

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget = DefaultTarget, method: ResampleMethod = ResampleMethod.DEFAULT, strategy: OverviewStrategy = OverviewStrategy.DEFAULT): RasterSource =
    GeoTiffReprojectRasterSource(dataPath, targetCRS, resampleTarget, method, strategy, targetCellType = targetCellType, baseTiff = Some(tiff))

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): RasterSource =
    GeoTiffReprojectRasterSource(dataPath, crs, resampleTarget, method, strategy, targetCellType = targetCellType, baseTiff = Some(tiff))

  def convert(targetCellType: TargetCellType): RasterSource =
    GeoTiffReprojectRasterSource(dataPath, crs, resampleTarget, resampleMethod, strategy, targetCellType = Some(targetCellType), baseTiff = Some(tiff))

  override def toString: String = s"GeoTiffReprojectRasterSource(${dataPath.value}, $crs, $resampleTarget, $resampleMethod)"
}

object GeoTiffReprojectRasterSource {
  def apply(
    dataPath: GeoTiffPath,
    crs: CRS,
    resampleTarget: ResampleTarget = DefaultTarget,
    resampleMethod: ResampleMethod = ResampleMethod.DEFAULT,
    strategy: OverviewStrategy = OverviewStrategy.DEFAULT,
    errorThreshold: Double = 0.125,
    targetCellType: Option[TargetCellType] = None,
    baseTiff: Option[MultibandGeoTiff] = None
  ): GeoTiffReprojectRasterSource = new GeoTiffReprojectRasterSource(dataPath, crs, resampleTarget, resampleMethod, strategy, errorThreshold, targetCellType, baseTiff)
}
