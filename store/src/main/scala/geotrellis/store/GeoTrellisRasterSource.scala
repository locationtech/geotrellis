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

import geotrellis.proj4._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster._
import geotrellis.layer._
import geotrellis.layer.filter._
import geotrellis.vector._

import jp.ne.opt.chronoscala.Imports._
import org.log4s.getLogger

import java.time.ZonedDateTime

case class Layer(id: LayerId, metadata: TileLayerMetadata[_], bandCount: Int) {
  /** GridExtent of the data pixels in the layer */
  def gridExtent: GridExtent[Long] = metadata.layout.createAlignedGridExtent(metadata.extent)
}

/**
  * Note: GeoTrellis AttributeStore does not store the band count for the layers by default,
  *       thus they need to be provided from application configuration.
  *
  * @param attributeStore GeoTrellis attribute store
  * @param dataPath       GeoTrellis catalog DataPath
  * @param sourceLayers   List of source layers
  * @param time           time slice, in case we're trying to read temporal layer slices
  * @param targetCellType The target cellType
  */
class GeoTrellisRasterSource(
  val attributeStore: AttributeStore,
  val dataPath: GeoTrellisPath,
  val sourceLayers: Stream[Layer],
  val targetCellType: Option[TargetCellType],
  val time: Option[ZonedDateTime],
  val timeMetadataKey: String = "times"
) extends RasterSource {
  @transient private[this] lazy val logger = getLogger

  def name: GeoTrellisPath = dataPath

  def this(attributeStore: AttributeStore, dataPath: GeoTrellisPath) =
    this(
      attributeStore,
      dataPath,
      GeoTrellisRasterSource.getSourceLayersByName(attributeStore, dataPath.layerName, dataPath.bandCount.getOrElse(1)),
      None,
      None
    )

  def this(dataPath: GeoTrellisPath) = this(AttributeStore(dataPath.value), dataPath)

  def layerId: LayerId = dataPath.layerId

  lazy val reader = CollectionLayerReader(attributeStore, dataPath.value)

  // read metadata directly instead of searching sourceLayers to avoid unneeded reads
  lazy val layerMetadata: TileLayerMetadata[_] = reader.attributeStore.readTileLayerMetadataErased(layerId)

  lazy val gridExtent: GridExtent[Long] = layerMetadata.layout.createAlignedGridExtent(layerMetadata.extent)

  val bandCount: Int = dataPath.bandCount.getOrElse(1)

  def crs: CRS = layerMetadata.crs

  def cellType: CellType = dstCellType.getOrElse(layerMetadata.cellType)

  def attributes: Map[String, String] = Map(
    "catalogURI" -> dataPath.value,
    "layerName"  -> layerId.name,
    "zoomLevel"  -> layerId.zoom.toString,
    "bandCount"  -> bandCount.toString
  ) ++ time.map(t => ("time", t.toString)).toMap

  /** GeoTrellis metadata doesn't allow to query a per band metadata by default. */
  def attributesForBand(band: Int): Map[String, String] = Map.empty

  def metadata: GeoTrellisMetadata = GeoTrellisMetadata(name, crs, bandCount, cellType, gridExtent, resolutions, attributes)

  // reference to this will fully initilze the sourceLayers stream
  lazy val resolutions: List[CellSize] = sourceLayers.map(_.gridExtent.cellSize).toList

  lazy val times: List[ZonedDateTime] = {
    val layerId = dataPath.layerId
    val header = attributeStore.readHeader[LayerHeader](layerId)
    if (header.keyClass.contains("SpaceTimeKey")) {
      attributeStore.read[List[ZonedDateTime]](layerId, "times").sorted
    } else {
      List.empty[ZonedDateTime]
    }
  }

  lazy val isTemporal: Boolean = times.nonEmpty

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] =
    GeoTrellisRasterSource.read(reader, layerId, layerMetadata, extent, bands, time).map(convertRaster)

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
    if (targetCRS != this.crs) {
      val reprojectOptions = ResampleTarget.toReprojectOptions(this.gridExtent, resampleTarget, method)
      val (closestLayerId, targetGridExtent) = GeoTrellisReprojectRasterSource.getClosestSourceLayer(targetCRS, sourceLayers, reprojectOptions, strategy)
      new GeoTrellisReprojectRasterSource(attributeStore, dataPath, closestLayerId, sourceLayers, targetGridExtent, targetCRS, resampleTarget, method, time = time, targetCellType = targetCellType)
    } else {
      // TODO: add unit tests for this in particular, the behavior feels murky
      resampleTarget match {
        case DefaultTarget =>
          // I think I was asked to do nothing
          this
        case resampleTarget =>
          val resampledGridExtent = resampleTarget(this.gridExtent)
          val closestLayerId = GeoTrellisRasterSource.getClosestResolution(sourceLayers.toList, resampledGridExtent.cellSize, strategy)(_.metadata.layout.cellSize).id
          new GeoTrellisResampleRasterSource(attributeStore, dataPath, closestLayerId, sourceLayers, resampledGridExtent, method, time, targetCellType)
      }
    }
  }

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): RasterSource = {
    val resampledGridExtent = resampleTarget(this.gridExtent)
    val closestLayerId = GeoTrellisRasterSource.getClosestResolution(sourceLayers.toList, resampledGridExtent.cellSize, strategy)(_.metadata.layout.cellSize).id
    new GeoTrellisResampleRasterSource(attributeStore, dataPath, closestLayerId, sourceLayers, resampledGridExtent, method, time, targetCellType)
  }

  def convert(targetCellType: TargetCellType): RasterSource =
    new GeoTrellisRasterSource(attributeStore, dataPath, sourceLayers, Some(targetCellType), time)

  override def toString: String = s"GeoTrellisRasterSource($dataPath, $layerId)"
}


object GeoTrellisRasterSource {
  // stable identifiers to match in a readTiles function
  private val SpatialKeyClass    = classOf[SpatialKey]
  private val SpaceTimeKeyClass  = classOf[SpaceTimeKey]
  private val TileClass          = classOf[Tile]
  private val MultibandTileClass = classOf[MultibandTile]

  def getClosestResolution[T](
    grids: Seq[T],
    cellSize: CellSize,
    strategy: OverviewStrategy = OverviewStrategy.DEFAULT
  )(implicit f: T => CellSize): T = {
    val sgrids = grids.sortBy(f(_))
    val resolutions = sgrids.map(f).toList
    val sourceResolution = OverviewStrategy.selectOverview(resolutions, cellSize, strategy)
    sgrids(sourceResolution)
  }

  /** Read metadata for all layers that share a name and sort them by their resolution */
  def getSourceLayersByName(attributeStore: AttributeStore, layerName: String, bandCount: Int): Stream[Layer] = {
    attributeStore.
      layerIds.
      filter(_.name == layerName).
      sortWith(_.zoom > _.zoom).
      toStream. // We will be lazy about fetching higher zoom levels
      map { id =>
        val metadata = attributeStore.readTileLayerMetadataErased(id)
        Layer(id, metadata, bandCount)
      }
  }

  def readTiles(
    reader: CollectionLayerReader[LayerId],
    layerId: LayerId,
    extent: Extent,
    bands: Seq[Int],
    time: Option[ZonedDateTime]
  ): Seq[(SpatialKey, MultibandTile)] with Metadata[TileLayerMetadata[SpatialKey]] = {
    def spatialTileRead =
      reader.query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
        .where(Intersects(extent))
        .result
        .withContext { _.map { case (key, tile) => (key, MultibandTile(tile)) } }

    def spatialMultibandTileRead =
      reader.query[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](layerId)
        .where(Intersects(extent))
        .result
        .withContext { _.map { case (key, tile) => (key, tile.subsetBands(bands)) } }

    def spaceTimeTileRead = {
      val query =
        reader
          .query[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](layerId)
          .where(Intersects(extent))

      time
        .fold(query)(t => query.where(At(t)))
        .result
        .withContext { _.map { case (key, tile) => (key, MultibandTile(tile)) } }
        .toSpatial
    }

    def spaceTimeMultibandTileRead = {
      val query =
        reader
          .query[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](layerId)
          .where(Intersects(extent))

      time
        .fold(query)(t => query.where(At(t)))
        .result
        .withContext { _.map { case (key, tile) => (key, tile.subsetBands(bands)) } }
        .toSpatial
    }

    val header = reader.attributeStore.readHeader[LayerHeader](layerId)

    if (!header.keyClass.contains("spark")) {
      (Class.forName(header.keyClass), Class.forName(header.valueClass)) match {
        case (SpatialKeyClass, TileClass)            => spatialTileRead
        case (SpatialKeyClass, MultibandTileClass)   => spatialMultibandTileRead
        case (SpaceTimeKeyClass, TileClass)          => spaceTimeTileRead
        case (SpaceTimeKeyClass, MultibandTileClass) => spaceTimeMultibandTileRead
        case _ =>
          throw new Exception(s"Unable to read single or multiband tiles from file: ${(header.keyClass, header.valueClass)}")
      }
    } else {
      /** Legacy GeoTrellis Layers compact */
      (header.keyClass, header.valueClass) match {
        case ("geotrellis.spark.SpatialKey", "geotrellis.raster.Tile")            => spatialTileRead
        case ("geotrellis.spark.SpatialKey", "geotrellis.raster.MultibandTile")   => spatialMultibandTileRead
        case ("geotrellis.spark.SpaceTimeKey", "geotrellis.raster.Tile")          => spaceTimeTileRead
        case ("geotrellis.spark.SpaceTimeKey", "geotrellis.raster.MultibandTile") => spaceTimeMultibandTileRead
        case _ =>
          throw new Exception(s"Unable to read single or multiband tiles from file: ${(header.keyClass, header.valueClass)}")
      }
    }
  }

  def readIntersecting(reader: CollectionLayerReader[LayerId], layerId: LayerId, metadata: TileLayerMetadata[_], extent: Extent, bands: Seq[Int], time: Option[ZonedDateTime]): Option[Raster[MultibandTile]] = {
    val tiles = readTiles(reader, layerId, extent, bands, time)
    tiles.sparseStitch(extent)
  }

  def read(reader: CollectionLayerReader[LayerId], layerId: LayerId, metadata: TileLayerMetadata[_], extent: Extent, bands: Seq[Int], time: Option[ZonedDateTime]): Option[Raster[MultibandTile]] = {
    val tiles = readTiles(reader, layerId, extent, bands, time)
    metadata.extent.intersection(extent) flatMap { intersectionExtent =>
      tiles.sparseStitch(intersectionExtent).map(_.crop(intersectionExtent))
    }
  }

  def apply(dataPath: GeoTrellisPath): GeoTrellisRasterSource = GeoTrellisRasterSource(dataPath, None, None)

  def apply(dataPath: GeoTrellisPath, time: Option[ZonedDateTime]): GeoTrellisRasterSource = GeoTrellisRasterSource(dataPath, time, None)

  def apply(dataPath: GeoTrellisPath, time: Option[ZonedDateTime], targetCellType: Option[TargetCellType]): GeoTrellisRasterSource = {
    val attributeStore = AttributeStore(dataPath.value)
    new GeoTrellisRasterSource(
      attributeStore,
      dataPath,
      GeoTrellisRasterSource.getSourceLayersByName(
        attributeStore, dataPath.layerName, dataPath.bandCount.getOrElse(1)
      ),
      targetCellType,
      time
    )
  }
}
