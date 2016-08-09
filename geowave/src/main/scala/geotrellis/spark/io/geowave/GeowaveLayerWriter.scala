package geotrellis.spark.io.geowave

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.util._
import geotrellis.vector.Extent

import com.typesafe.scalalogging.slf4j._
import mil.nga.giat.geowave.adapter.raster.adapter.merge.RasterTileRowTransform
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.core.store.adapter.statistics.StatsCompositionTool
import mil.nga.giat.geowave.core.store.data.VisibilityWriter
import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions
import mil.nga.giat.geowave.core.store.util.DataStoreUtils
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import mil.nga.giat.geowave.datastore.accumulo.util._
import mil.nga.giat.geowave.datastore.accumulo.util.AccumuloUtils
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.data.{ Key, Value }
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.processing.CoverageProcessor
import org.geotools.factory.{ GeoTools, Hints }
import org.opengis.coverage.grid.GridCoverage
import org.opengis.parameter.ParameterValueGroup

import java.io.{ DataInputStream, DataOutputStream, File }
import java.util.UUID
import javax.imageio.ImageIO
import javax.media.jai.{ ImageLayout, JAI }
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.reflect._

import resource._
import spray.json._


object GeowaveLayerWriter extends LazyLogging {

  def write[
    K: ClassTag,
    V: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](
    coverageName: String,
    rdd: RDD[(K, V)] with Metadata[M],
    as: GeowaveAttributeStore,
    accumuloWriter: AccumuloWriteStrategy
  ): Unit = {
    val metadata = implicitly[ClassTag[K]].toString match {
      case "geotrellis.spark.SpatialKey" => rdd.metadata.asInstanceOf[TileLayerMetadata[SpatialKey]]
      case t: String => throw new Exception(s"Unsupported Key Type: $t")
    }

    val crs = metadata.crs
    val mt = metadata.mapTransform
    val cellType = metadata.cellType.toString
    val specimen = rdd.first

    /* Construct (Multiband|)Tile to GridCoverage2D conversion function */
    val geotrellisKvToGeotools: (((K, V)) => GridCoverage2D) = {
      specimen match {
        case (_: SpatialKey, _: Tile) => {
          case (k: K, v: V) =>
            val extent = mt(k.asInstanceOf[SpatialKey]).reproject(crs, LatLng)
            val tile = v.asInstanceOf[Tile]
            ProjectedRaster(Raster(tile, extent), LatLng).toGridCoverage2D
        }
        case (_: SpatialKey, _: MultibandTile) => {
          case (k: K, v: V) =>
            val extent = mt(k.asInstanceOf[SpatialKey]).reproject(crs, LatLng)
            val tile = v.asInstanceOf[MultibandTile]
            ProjectedRaster(Raster(tile, extent), LatLng).toGridCoverage2D
        }
      }
    }
    val image = geotrellisKvToGeotools(specimen)

    val pluginOptions = new DataStorePluginOptions
    pluginOptions.setFactoryOptions(as.aro)

    val configOptions = pluginOptions.getFactoryOptionsAsMap

    val geotrellisKvToGeoWaveKv: Iterable[(K, V)] => Iterable[(Key, Value)] = { pair =>
      {
        val gwMetadata = new java.util.HashMap[String, String](); gwMetadata.put("cellType", cellType)
        configOptions.put(
          GeoWaveStoreFinder.STORE_HINT_OPTION.getName(),
          "accumulo")

        /* Produce mosaic from all of the tiles in this partition */
        val sources = new java.util.ArrayList[GridCoverage2D]
        pair.map({ case kv => sources.add(geotrellisKvToGeotools(kv)); Unit }).toList
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
            image,
            256, false, false,
            Array.fill[Array[Double]](image.getNumSampleDimensions)(Array(0.0))) // image only used for sample and color metadata, not data, overriding default merge strategy because geotrellis data is already tiled (non-overlapping)

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

            adapter.convertToIndex(index, image).foreach { x => (accumuloKvs += (kvGen.constructKeyValuePairs(adapter.getAdapterId.getBytes, x).toList.map { kv => (kv.getKey, kv.getValue) })) }
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

    // make sure adapter get written
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
    val iterators = ops.listIterators(tableName)
    iterators.foreach({ kv =>
      if (kv._1.startsWith(RasterTileRowTransform.TRANSFORM_NAME)) {
        ops.removeIterator(tableName, kv._1, kv._2)
      }
    })
  }
}

class GeowaveLayerWriter(
  val attributeStore: GeowaveAttributeStore,
  val accumuloWriter: AccumuloWriteStrategy
)(implicit sc: SparkContext)
    extends LayerWriter[LayerId] with LazyLogging {

  protected def _write[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat: GetComponent[?, Bounds[K]]](
    layerId: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    keyIndex: KeyIndex[K]): Unit = {

    val LayerId(coverageName, zoom) = layerId
    val specimen = rdd.first

    // Perform checks
    if (zoom > 0) logger.warn("The zoom level is mostly ignored because GeoWave does its own pyramiding")
    specimen._1 match {
      case _: SpatialKey =>
      case _ => throw new Exception(s"Unsupported Key Type: ${implicitly[ClassTag[K]].toString}")
    }
    specimen._2 match {
      case _: Tile =>
      case _: MultibandTile =>
      case _ => throw new Exception(s"Unsupported Value Type: ${implicitly[ClassTag[V]].toString}")
    }

    GeowaveLayerWriter.write(coverageName, rdd, attributeStore, accumuloWriter)
  }

}
