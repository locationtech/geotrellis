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
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloAttributeStore
import geotrellis.util._
import geotrellis.util.annotations.experimental
import geotrellis.vector.Extent

import org.locationtech.jts.geom._
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


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeowaveAttributeStore {

  /** $experimental */
  @experimental def accumuloRequiredOptions(
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

  /** $experimental */
  @experimental def basicOperations(
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

  /** $experimental */
  @experimental def adapters(bao: BasicAccumuloOperations): Array[RasterDataAdapter] = {
    val adapters = new AccumuloAdapterStore(bao).getAdapters
    val retval = adapters.asScala
      .map(_.asInstanceOf[RasterDataAdapter])
      .toArray

    adapters.close ; retval
  }

  /** $experimental */
  @experimental def primaryIndex = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex()

  /** $experimental */
  @experimental def subStrategies(idx: PrimaryIndex) = idx
    .getIndexStrategy
    .asInstanceOf[HierarchicalNumericIndexStrategy]
    .getSubStrategies

}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeowaveAttributeStore(
  val zookeepers: String,
  val accumuloInstance: String,
  val accumuloUser: String,
  val accumuloPass: String,
  val geowaveNamespace: String
) extends DiscreteLayerAttributeStore with LazyLogging {

  logger.error("GeoWave support is experimental")

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

  /** $experimental */
  @experimental def delete(tableName: String) =
    connector.tableOperations.delete(tableName)

  /** $experimental */
  @experimental def boundingBoxes(): Map[ByteArrayId, BoundingBoxDataStatistics[Any]] = {
    adapters.map({ adapter =>
      val adapterId = adapter.getAdapterId
      val bboxId = BoundingBoxDataStatistics.STATS_ID
      val bbox = dataStatisticsStore
        .getDataStatistics(adapterId, bboxId)
        .asInstanceOf[BoundingBoxDataStatistics[Any]]

      (adapterId, bbox)
    }).toMap
  }

  /** $experimental */
  @experimental def leastZooms(): Map[ByteArrayId, Int] = {
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

  /** $experimental */
  @experimental def primaryIndex = GeowaveAttributeStore.primaryIndex

  /** $experimental */
  @experimental def adapters = GeowaveAttributeStore.adapters(basicAccumuloOperations)

  /** $experimental */
  @experimental def subStrategies = GeowaveAttributeStore.subStrategies(primaryIndex)

  /** $experimental */
  @experimental def delete(layerId: LayerId, attributeName: String): Unit =
    delegate.delete(layerId, attributeName)

  /** $experimental */
  @experimental def delete(layerId: LayerId): Unit = delegate.delete(layerId)

  /** $experimental */
  @experimental def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] =
    delegate.readAll[T](attributeName)

  /** $experimental */
  @experimental def read[T: JsonFormat](layerId: LayerId, attributeName: String): T =
    delegate.read[T](layerId, attributeName)

  /** $experimental */
  @experimental def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit =
    delegate.write[T](layerId, attributeName, value)

  /** $experimental */
  @experimental def availableAttributes(layerId: LayerId) =
    delegate.availableAttributes(layerId)

  /**
    * Use GeoWave to see whether a layer really exists.
    */
  @experimental private def gwLayerExists(layerId: LayerId): Boolean = {
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
    * $experimental Answer whether a layer exists (either in GeoWave
    * or only in the AttributeStore).
    */
  @experimental def layerExists(layerId: LayerId): Boolean =
    gwLayerExists(layerId) || delegate.layerExists(layerId)

  /**
    * Use GeoWave to get a list of actual LayerIds.
    */
  @experimental private def gwLayerIds: Seq[LayerId] = {
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
    * $experimental Return a complete list of LayerIds (those
    * associated with actual GeoWave layers, as well as those only
    * recorded in the AttributeStore).
    */
  @experimental def layerIds: Seq[LayerId] =
    (gwLayerIds ++ delegate.layerIds).distinct
}
