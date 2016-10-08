package geotrellis.spark.io.geowave

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.vector.Extent
import geotrellis.spark.io.accumulo.AccumuloAttributeStore

import com.typesafe.scalalogging.LazyLogging
import com.vividsolutions.jts.geom._
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.geotime.store.statistics.BoundingBoxDataStatistics
import mil.nga.giat.geowave.core.index.ByteArrayId
import mil.nga.giat.geowave.core.index.HierarchicalNumericIndexStrategy
import mil.nga.giat.geowave.core.index.HierarchicalNumericIndexStrategy.SubStrategy
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.core.store.index.{PrimaryIndex, CustomIdIndex}
import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions
import mil.nga.giat.geowave.core.store.query.QueryOptions
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary.AccumuloSecondaryIndexDataStore
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloRequiredOptions
import mil.nga.giat.geowave.mapreduce.input.{GeoWaveInputKey, GeoWaveInputFormat}
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.client.{TableNotFoundException, ZooKeeperInstance}
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.SparkContext._
import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.opengis.coverage.grid.GridCoverage
import org.opengis.parameter.GeneralParameterValue

import scala.collection.JavaConverters._
import scala.util.Try

import spray.json._
import spray.json.DefaultJsonProtocol._


object GeowaveAttributeStore {

  def accumuloRequiredOptions(
    zookeepers: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  ): AccumuloRequiredOptions = {
    val aro = new AccumuloRequiredOptions
    aro.setZookeeper(zookeepers)
    aro.setInstance(accumuloInstance)
    aro.setUser(accumuloUser)
    aro.setPassword(accumuloPass)
    aro.setGeowaveNamespace(geowaveNamespace)
    aro
  }

  def basicOperations(
    zookeepers: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  ): BasicAccumuloOperations = {
    return new BasicAccumuloOperations(
      zookeepers,
      accumuloInstance,
      accumuloUser,
      accumuloPass,
      geowaveNamespace)
  }

  def adapters(bao: BasicAccumuloOperations): Array[RasterDataAdapter] = {
    val adapters = new AccumuloAdapterStore(bao).getAdapters
    val retval = adapters.asScala
      .map(_.asInstanceOf[RasterDataAdapter])
      .toArray

    adapters.close ; retval
  }

  def primaryIndex = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex()

  def subStrategies(idx: PrimaryIndex) = idx
    .getIndexStrategy
    .asInstanceOf[HierarchicalNumericIndexStrategy]
    .getSubStrategies

}

class GeowaveAttributeStore(
  val zookeepers: String,
  val accumuloInstance: String,
  val accumuloUser: String,
  val accumuloPass: String,
  val geowaveNamespace: String
) extends DiscreteLayerAttributeStore with LazyLogging {

  val zkInstance = (new ZooKeeperInstance(accumuloInstance, zookeepers))
  val token = new PasswordToken(accumuloPass)
  val connector = zkInstance.getConnector(accumuloUser, token)
  val delegate = AccumuloAttributeStore(connector, s"${geowaveNamespace}_ATTR")

  val basicAccumuloOperations = GeowaveAttributeStore.basicOperations(
    zookeepers,
    accumuloInstance,
    accumuloUser,
    accumuloPass,
    geowaveNamespace: String
  )
  val accumuloRequiredOptions = GeowaveAttributeStore.accumuloRequiredOptions(
    zookeepers,
    accumuloInstance,
    accumuloUser,
    accumuloPass,
    geowaveNamespace
  )
  val dataStore = new AccumuloDataStore(basicAccumuloOperations)
  val dataStatisticsStore = new AccumuloDataStatisticsStore(basicAccumuloOperations)

  def delete(tableName: String) =
    connector.tableOperations.delete(tableName)

  def boundingBoxes(): Map[ByteArrayId, BoundingBoxDataStatistics[Any]] = {
    adapters.map({ adapter =>
      val adapterId = adapter.getAdapterId
      val bboxId = BoundingBoxDataStatistics.STATS_ID
      val bbox = dataStatisticsStore
        .getDataStatistics(adapterId, bboxId)
        .asInstanceOf[BoundingBoxDataStatistics[Any]]

      (adapterId, bbox)
    }).toMap
  }

  def leastZooms(): Map[ByteArrayId, Int] = {
    adapters.map({ adapter =>
      val adapterId = adapter.getAdapterId
      val bbox = boundingBoxes.getOrElse(adapterId, throw new Exception(s"Unknown Adapter Id $adapterId"))
      val zoom = bbox match {
        case null => {
          logger.warn(s"$adapterId has a broken bounding box")
          0
        }
        case _ => {
          val substrats = subStrategies
          val width = bbox.getMaxX - bbox.getMinX
          val height = bbox.getMaxY - bbox.getMinY
          val zoom = (0 to subStrategies.length).toIterator.filter({ i =>
            val substrat = substrats(i)
            val ranges = substrat.getIndexStrategy.getHighestPrecisionIdRangePerDimension
            ((ranges(0) <= width) && (ranges(1) <= height))
          }).next

          zoom
        }
      }

      (adapterId, zoom)
    }).toMap
  }

  def primaryIndex = GeowaveAttributeStore.primaryIndex
  def adapters = GeowaveAttributeStore.adapters(basicAccumuloOperations)
  def subStrategies = GeowaveAttributeStore.subStrategies(primaryIndex)

  def delete(layerId: LayerId, attributeName: String): Unit =
    delegate.delete(layerId, attributeName)

  def delete(layerId: LayerId): Unit = delegate.delete(layerId)

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] =
    delegate.readAll[T](attributeName)

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T =
    delegate.read[T](layerId, attributeName)

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit =
    delegate.write[T](layerId, attributeName, value)

  def availableAttributes(layerId: LayerId) =
    delegate.availableAttributes(layerId)

  /**
    * Use GeoWave to see whether a layer really exists.
    */
  private def gwLayerExists(layerId: LayerId): Boolean = {
    val LayerId(name, zoom) = layerId
    val candidateAdapters = adapters.filter(_.getCoverageName == name)

    if (candidateAdapters.nonEmpty) {
      val adapterId = candidateAdapters.head.getAdapterId
      val leastZoom = leastZooms.getOrElse(
        adapterId,
        throw new Exception(s"Unknown Adapter Id $adapterId")
      )

      ((leastZoom <= zoom) && (zoom < subStrategies.length))
    } else false
  }

  /**
    * Answer whether a layer exists (either in GeoWave or only in the
    * AttributeStore).
    */
  def layerExists(layerId: LayerId): Boolean =
    gwLayerExists(layerId) || delegate.layerExists(layerId)

  /**
    * Use GeoWave to get a list of actual LayerIds.
    */
  private def gwLayerIds: Seq[LayerId] = {
    val list =
      for (
        adapter <- adapters;
        zoom <- {
          val adapterId = adapter.getAdapterId
          val leastZoom = leastZooms.getOrElse(
            adapter.getAdapterId,
            throw new Exception(s"Unknown Adapter Id $adapterId")
          )

          (leastZoom until subStrategies.length)
        }
      ) yield LayerId(adapter.getCoverageName, zoom)

    list.distinct
  }

  /**
    * Return a complete list of LayerIds (those associated with actual
    * GeoWave layers, as well as those only recorded in the
    * AttributeStore).
    */
  def layerIds: Seq[LayerId] =
    (gwLayerIds ++ delegate.layerIds).distinct
}
