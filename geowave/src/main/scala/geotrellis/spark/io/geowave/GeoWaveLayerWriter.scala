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

package geotrellis.spark.io.geowave

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.layers.io.avro._
import geotrellis.layers.io.index.KeyIndex
import geotrellis.util._
import geotrellis.util.annotations.experimental
import geotrellis.vector.Extent
import com.typesafe.scalalogging.LazyLogging
import mil.nga.giat.geowave.adapter.raster.adapter.merge.RasterTileRowTransform
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.core.geotime.index.dimension._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.index.sfc.SFCDimensionDefinition
import mil.nga.giat.geowave.core.index.sfc.SFCFactory.SFCType
import mil.nga.giat.geowave.core.index.sfc.tiered.TieredSFCIndexFactory
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import mil.nga.giat.geowave.datastore.accumulo.util._
import mil.nga.giat.geowave.datastore.accumulo.util.AccumuloUtils
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.data.{Key, Value}
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.processing.CoverageProcessor
import org.geotools.util.factory.{GeoTools, Hints}
import org.opengis.coverage.grid.GridCoverage
import org.opengis.parameter.ParameterValueGroup
import java.io.{DataInputStream, DataOutputStream, File}
import java.util.UUID

import geotrellis.layers.{LayerId, Metadata, TileLayerMetadata}
import javax.imageio.ImageIO
import javax.media.jai.{ImageLayout, JAI}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.reflect._
import mil.nga.giat.geowave.core.store.util.DataStoreUtils
import resource._
import spray.json._
import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions
import mil.nga.giat.geowave.core.store.adapter.statistics.StatsCompositionTool
import mil.nga.giat.geowave.core.store.data.VisibilityWriter

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoWaveLayerWriter extends LazyLogging {

  /** $experimental */
  @experimental def write[
    K <: SpatialKey: ClassTag,
    V: TileOrMultibandTile: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](
    coverageName: String,
    bits: Int,
    rdd: RDD[(K, V)] with Metadata[M],
    as: GeoWaveAttributeStore,
    accumuloWriter: AccumuloWriteStrategy
  ): Unit = {
    val metadata = rdd.metadata.asInstanceOf[TileLayerMetadata[SpatialKey]]

    val crs = metadata.crs
    val mt = metadata.mapTransform
    val cellType = metadata.cellType.toString
    val specimen = rdd.first

    /* Construct (Multiband|)Tile to GridCoverage2D conversion function */
    val rectify = GeoWaveUtil.rectify(bits)_
    val geotrellisKvToGeotools: ((K, V)) => GridCoverage2D = {
      case (k: SpatialKey, _tile: V) =>
        val Extent(minX, minY, maxX, maxY) = mt(k.asInstanceOf[SpatialKey]).reproject(crs, LatLng)
        val extent = Extent(rectify(minX), rectify(minY), rectify(maxX), rectify(maxY))

        _tile match {
          case tile: Tile =>
            ProjectedRaster(Raster(tile, extent), LatLng).toGridCoverage2D
          case tile: MultibandTile =>
            ProjectedRaster(Raster(tile, extent), LatLng).toGridCoverage2D
        }
    }
    val image = geotrellisKvToGeotools(specimen)

    val pluginOptions = new DataStorePluginOptions
    pluginOptions.setFactoryOptions(as.accumuloRequiredOptions)

    val configOptions = pluginOptions.getOptionsAsMap

    val geotrellisKvToGeoWaveKv: Iterable[(K, V)] => Iterable[(Key, Value)] = { pairs =>
      {
        val gwMetadata = new java.util.HashMap[String, String](); gwMetadata.put("cellType", cellType)

        /* Produce mosaic from all of the tiles in this partition */
        val sources = new java.util.ArrayList(pairs.map(geotrellisKvToGeotools).asJavaCollection)
        val accumuloKvs = ListBuffer[Iterable[(Key, Value)]]()

        /* Objects for writing into GeoWave */
        if (sources.size > 0) {
          val processor = CoverageProcessor.getInstance(GeoTools.getDefaultHints())
          val param = processor.getOperation("Mosaic").getParameters()
          val hints = new Hints
          val imageLayout = new ImageLayout
          imageLayout.setTileHeight(256)
          imageLayout.setTileWidth(256)

          logger.info(s"partition size = ${sources.size}")
          param.parameter("Sources").setValue(sources)
          hints.put(JAI.KEY_IMAGE_LAYOUT, imageLayout)

          val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex()

          val image = processor.doOperation(param, hints).asInstanceOf[GridCoverage2D]
          val adapter = new RasterDataAdapter(
            coverageName,
            gwMetadata,
            image,  // image only used for sample and color metadata, not data
            256, false, false,
            Array.fill[Array[Double]](image.getNumSampleDimensions)(Array(0.0))) // overriding default merge strategy because geotrellis data is already tiled (non-overlapping)

          for (
            statsAggregator <- managed(new StatsCompositionTool(new DataStoreStatisticsProvider(
              adapter,
              index,
              true),
              GeoWaveStoreFinder.createDataStatisticsStore(configOptions)))
          ) {
            val kvGen = new AccumuloKeyValuePairGenerator[GridCoverage](
              adapter,
              index,
              statsAggregator,
              DataStoreUtils.UNCONSTRAINED_VISIBILITY.asInstanceOf[VisibilityWriter[GridCoverage]])

            adapter.convertToIndex(index, image).asScala.foreach({ i =>
              val keyValues = kvGen.constructKeyValuePairs(adapter.getAdapterId.getBytes, i).asScala.toList
              val keyValuePairs = keyValues.map({ kv => (kv.getKey, kv.getValue) })
              accumuloKvs += keyValuePairs
            })
          }
        }
        accumuloKvs.foldLeft(Iterable[(Key, Value)]())(_ ++ _)
      }
    }

    val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex()
    val indexName = index.getId.getString
    val tableName = AccumuloUtils.getQualifiedTableName(as.geowaveNamespace, indexName)

    val gwMetadata = new java.util.HashMap[String, String](); gwMetadata.put("cellType", cellType)
    val basicOperations = new BasicAccumuloOperations(
      as.zookeepers,
      as.accumuloInstance,
      as.accumuloUser,
      as.accumuloPass,
      as.geowaveNamespace)
    val adapter = new RasterDataAdapter(
      coverageName,
      gwMetadata,
      image,
      256, true, false,
      Array.fill[Array[Double]](image.getNumSampleDimensions)(Array(0.0))
    )

    // make sure adapter gets written
    val adapterStore = new AccumuloAdapterStore(basicOperations)
    adapterStore.addAdapter(adapter)

    // make sure index gets written
    val indexStore = new AccumuloIndexStore(basicOperations)
    indexStore.addIndex(index)

    // make sure adapter and index are associated together in the mapping store
    val mappingStore = new AccumuloAdapterIndexMappingStore(basicOperations)
    mappingStore.addAdapterIndexMapping(new AdapterToIndexMapping(adapter.getAdapterId, Array(index.getId)))

    AccumuloUtils.attachRowMergingIterators(
      adapter,
      basicOperations,
      new AccumuloOptions,
      index.getIndexStrategy().getNaturalSplits(),
      indexName)
    accumuloWriter.write(
      rdd
        .sortBy({ case (k, v) => SpatialKey.keyToTup(k.asInstanceOf[SpatialKey]) })
        .groupBy({ case (k, _) => k.asInstanceOf[SpatialKey]._1 })
        .map(_._2)
        .mapPartitions({ partitions => partitions.map(geotrellisKvToGeoWaveKv) })
        .flatMap(i => i), // first argument to accumuloWriter.write
      AccumuloInstance(
        as.accumuloInstance,
        as.zookeepers,
        as.accumuloUser,
        new PasswordToken(as.accumuloPass)
      ), // second argument
      tableName // third argument
    )

    val conn = ConnectorPool.getInstance.getConnector(
      as.zookeepers,
      as.accumuloInstance,
      as.accumuloUser,
      as.accumuloPass
    )

    // compact
    val ops = conn.tableOperations()
    ops.compact(tableName, null, null, true, true)

    // detach iterator
    val iterators = ops.listIterators(tableName).asScala
    iterators.foreach { kv =>
      if (kv._1.startsWith(RasterTileRowTransform.TRANSFORM_NAME)) {
        ops.removeIterator(tableName, kv._1, kv._2)
      }
    }
  }
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeoWaveLayerWriter(
  val attributeStore: GeoWaveAttributeStore,
  val accumuloWriter: AccumuloWriteStrategy
)(implicit sc: SparkContext)
    extends LazyLogging {

  logger.error("GeoWave support is experimental")

  /** $experimental */
  @experimental def write[
    K <: SpatialKey: ClassTag,
    V: TileOrMultibandTile: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, layer: RDD[(K, V)] with Metadata[M], bits: Int = 0): Unit =
    layer.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        _write[K, V, M](id, layer, bits)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  /** $experimental */
  @experimental protected def _write[
    K <: SpatialKey: ClassTag,
    V: TileOrMultibandTile: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](
    layerId: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    bits: Int
  ): Unit = {
    val LayerId(coverageName, tier) = layerId
    val specimen = rdd.first

    if (tier != 0)
      logger.warn(s"GeoWave has its own notion of levels/tiering, so $tier in $layerId will be ignored")

    if (bits <= 0)
      logger.warn("It is highly recommended that you specify a bit precision when writing into GeoWave")

    GeoWaveLayerWriter.write(
      coverageName,
      (if (bits <= 0) 0; else bits),
      rdd,
      attributeStore,
      accumuloWriter
    )
  }

}
