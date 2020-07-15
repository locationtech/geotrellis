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

package geotrellis.store

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.proj4._
import geotrellis.raster.io.geotiff.OverviewStrategy

import org.log4s._

import scala.io.AnsiColor._
import java.time.ZonedDateTime

class GeoTrellisReprojectRasterSource(
  val attributeStore: AttributeStore,
  val dataPath: GeoTrellisPath,
  val layerId: LayerId,
  val sourceLayers: Stream[Layer],
  val gridExtent: GridExtent[Long],
  val crs: CRS,
  val resampleTarget: ResampleTarget = DefaultTarget,
  val resampleMethod: ResampleMethod = ResampleMethod.DEFAULT,
  val strategy: OverviewStrategy = OverviewStrategy.DEFAULT,
  val errorThreshold: Double = 0.125,
  val time: Option[ZonedDateTime] = None,
  val targetCellType: Option[TargetCellType]
) extends RasterSource {
  @transient private[this] lazy val logger = getLogger

  def name: GeoTrellisPath = dataPath

  lazy val reader = CollectionLayerReader(attributeStore, dataPath.value)

  lazy val resolutions: List[CellSize] = sourceLayers.map { layer =>
    ReprojectRasterExtent(layer.gridExtent, Transform(layer.metadata.crs, crs), Reproject.Options.DEFAULT).cellSize
  }.toList

  lazy val sourceLayer: Layer = sourceLayers.find(_.id == layerId).get

  def bandCount: Int = sourceLayer.bandCount

  def cellType: CellType = dstCellType.getOrElse(sourceLayer.metadata.cellType)

  def attributes: Map[String, String] = Map(
    "catalogURI" -> dataPath.value,
    "layerName" -> layerId.name,
    "zoomLevel" -> layerId.zoom.toString,
    "bandCount" -> bandCount.toString
  ) ++ time.map(t => ("time", t.toString)).toMap

  /** GeoTrellis metadata doesn't allow to query a per band metadata by default. */
  def attributesForBand(band: Int): Map[String, String] = Map.empty

  def metadata: GeoTrellisMetadata = GeoTrellisMetadata(name, crs, bandCount, cellType, gridExtent, resolutions, attributes)

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val transform = Transform(sourceLayer.metadata.crs, crs)
    val backTransform = Transform(crs, sourceLayer.metadata.crs)
    for {
      subExtent <- this.extent.intersection(extent)
      targetRasterExtent = this.gridExtent.createAlignedRasterExtent(subExtent)
      sourceExtent = targetRasterExtent.extent.reprojectAsPolygon(backTransform, 0.001).getEnvelopeInternal
      sourceRegion = sourceLayer.metadata.layout.createAlignedGridExtent(sourceExtent)
      _ = {
        lazy val tileBounds = sourceLayer.metadata.mapTransform.extentToBounds(sourceExtent)
        lazy val pixelsRead = (tileBounds.size * sourceLayer.metadata.layout.tileCols * sourceLayer.metadata.layout.tileRows).toDouble
        lazy val pixelsQueried = targetRasterExtent.cols.toDouble * targetRasterExtent.rows.toDouble

        def msg = s"""
          |${GREEN}Read($extent)${RESET} =
          |\t${BOLD}FROM${RESET} ${dataPath.toString} ${sourceLayer.id}
          |\t${BOLD}SOURCE${RESET} $sourceExtent ${sourceLayer.metadata.cellSize} @ ${sourceLayer.metadata.crs}
          |\t${BOLD}TARGET${RESET} ${targetRasterExtent.extent} ${targetRasterExtent.cellSize} @ ${crs}
          |\t${BOLD}READ${RESET} ${pixelsRead/pixelsQueried} read/query ratio for ${tileBounds.size} tiles
        """.stripMargin
        if (tileBounds.size < 1024) // Assuming 256x256 tiles this would be a very large request
          logger.debug(msg)
        else
          logger.warn(msg + " (large read)")
      }
      raster <- GeoTrellisRasterSource.readIntersecting(reader, layerId, sourceLayer.metadata, sourceExtent, bands, time)
    } yield {
      val reprojected = raster.reproject(
        targetRasterExtent,
        transform,
        backTransform,
        ResampleTarget.toReprojectOptions(targetRasterExtent.toGridType[Long], resampleTarget, resampleMethod)
      )
      convertRaster(reprojected)
    }
  }

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] =
    bounds
      .intersection(this.dimensions)
      .map(gridExtent.extentFor(_).buffer(- cellSize.width / 2, - cellSize.height / 2))
      .flatMap(read(_, bands))

  override def readExtents(extents: Traversable[Extent], bands: Seq[Int]): Iterator[Raster[MultibandTile]] =
    extents.toIterator.flatMap(read(_, bands))

  override def readBounds(bounds: Traversable[GridBounds[Long]], bands: Seq[Int]): Iterator[Raster[MultibandTile]] =
    bounds.toIterator.flatMap(_.intersection(this.dimensions).flatMap(read(_, bands)))

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget = DefaultTarget, method: ResampleMethod = ResampleMethod.DEFAULT, strategy: OverviewStrategy = OverviewStrategy.DEFAULT): RasterSource = {
    if (targetCRS == sourceLayer.metadata.crs) {
      val resampledGridExtent = resampleTarget(this.sourceLayer.gridExtent)
      val closestLayer = GeoTrellisRasterSource.getClosestResolution(sourceLayers, resampledGridExtent.cellSize, strategy)(_.metadata.layout.cellSize)
      // TODO: if closestLayer is w/in some marging of desired CellSize, return GeoTrellisRasterSource instead
      new GeoTrellisResampleRasterSource(attributeStore, dataPath, closestLayer.id, sourceLayers, resampledGridExtent, resampleMethod, time, targetCellType)
    } else {
      // Pick new layer ID
      val (_, gridExtent) =
        GeoTrellisReprojectRasterSource
          .getClosestSourceLayer(
            targetCRS,
            sourceLayers,
            ResampleTarget.toReprojectOptions(this.gridExtent, resampleTarget, resampleMethod),
            strategy
          )
      new GeoTrellisReprojectRasterSource(
        attributeStore,
        dataPath,
        layerId,
        sourceLayers,
        gridExtent,
        targetCRS,
        resampleTarget,
        resampleMethod,
        time = time,
        targetCellType = targetCellType
      )
    }
  }

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): RasterSource = {
    val newReprojectOptions = ResampleTarget.toReprojectOptions(this.gridExtent, resampleTarget, method)
    val (closestLayerId, newGridExtent) = GeoTrellisReprojectRasterSource.getClosestSourceLayer(crs, sourceLayers, newReprojectOptions, strategy)
    new GeoTrellisReprojectRasterSource(attributeStore, dataPath, closestLayerId, sourceLayers, newGridExtent, crs, resampleTarget, method, time = time, targetCellType = targetCellType)
  }

  def convert(targetCellType: TargetCellType): RasterSource =
    new GeoTrellisReprojectRasterSource(attributeStore, dataPath, layerId, sourceLayers, gridExtent, crs, resampleTarget, resampleMethod, time = time, targetCellType = Some(targetCellType))

  override def toString: String = s"GeoTrellisReprojectRasterSource(${dataPath.value},$layerId,$crs,$gridExtent,$resampleMethod)"
}

object GeoTrellisReprojectRasterSource {
  /** Pick the closest source layer and decide what its GridExtent should be based on Reproject.Options
   * Assumes that all layers in source Pyramid share the same CRS and the highest resolution layer is at the head.
   */
  private[store] def getClosestSourceLayer(
    targetCRS: CRS,
    sourcePyramid: Stream[Layer],
    options: Reproject.Options,
    strategy: OverviewStrategy
  ): (LayerId, GridExtent[Long]) = {
    // most resolute layer
    val baseLayer: Layer = sourcePyramid.minBy(_.metadata.cellSize.resolution)
    val sourceCRS: CRS = baseLayer.metadata.crs

    if (options.targetRasterExtent.isDefined) {
      val targetGrid: GridExtent[Long] = options.targetRasterExtent.get.toGridType[Long]
      val sourceGrid: GridExtent[Long] = ReprojectRasterExtent(targetGrid, targetCRS, sourceCRS)
      val sourceLayer = GeoTrellisRasterSource.getClosestResolution(sourcePyramid, sourceGrid.cellSize, strategy)(_.metadata.layout.cellSize)
      (sourceLayer.id, targetGrid)

    } else if (options.parentGridExtent.isDefined) {
      val targetGridAlignment: GridExtent[Long] = options.parentGridExtent.get
      val sourceGridInTargetCrs: GridExtent[Long] = ReprojectRasterExtent(baseLayer.gridExtent, sourceCRS, targetCRS)
      val sourceLayer: Layer = {
        // we know the target pixel grid but we don't know which is the closest source resolution to it
        // we're going to use the same heuristic we use when reprojecting without target CellSize backwards
        val provisional: GridExtent[Long] = targetGridAlignment.createAlignedGridExtent(sourceGridInTargetCrs.extent)
        val aproximateSourceCellSize: CellSize = {
          val newExtent = baseLayer.metadata.extent
          val distance = newExtent.northWest.distance(newExtent.southEast)
          val cols: Double = provisional.extent.width / provisional.cellwidth
          val rows: Double = provisional.extent.height / provisional.cellheight
          val pixelSize = distance / math.sqrt(cols * cols + rows * rows)
          CellSize(pixelSize, pixelSize)
        }
        GeoTrellisRasterSource.getClosestResolution(sourcePyramid, aproximateSourceCellSize, strategy)(_.metadata.layout.cellSize)
      }
      val gridExtent: GridExtent[Long] = ReprojectRasterExtent(sourceLayer.gridExtent, sourceLayer.metadata.crs, targetCRS, options)
      (sourceLayer.id, gridExtent)

    } else if (options.targetCellSize.isDefined) {
      val targetCellSize = options.targetCellSize.get
      val sourceGridInTargetCrs: GridExtent[Long] = ReprojectRasterExtent(baseLayer.gridExtent, sourceCRS, targetCRS)
      val aproximateSourceCellSize: CellSize = {
        val newExtent = baseLayer.metadata.extent
        val distance = newExtent.northWest.distance(newExtent.southEast)
        val cols: Double = sourceGridInTargetCrs.extent.width / targetCellSize.width
        val rows: Double = sourceGridInTargetCrs.extent.height / targetCellSize.height
        val pixelSize = distance / math.sqrt(cols * cols + rows * rows)
        CellSize(pixelSize, pixelSize)
      }
      val sourceLayer = GeoTrellisRasterSource.getClosestResolution(sourcePyramid, aproximateSourceCellSize, strategy)(_.metadata.layout.cellSize)
      val gridExtent: GridExtent[Long] = ReprojectRasterExtent(sourceLayer.gridExtent, sourceLayer.metadata.crs, targetCRS, options)
      (sourceLayer.id, gridExtent)

    } else { // do your worst ... or best !
      val targetGrid: GridExtent[Long] = ReprojectRasterExtent(baseLayer.gridExtent, sourceCRS, targetCRS)
      (sourcePyramid.head.id, targetGrid)
    }
  }
}
