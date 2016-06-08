package geotrellis.spark.io.geowave

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.vector.Extent

import com.vividsolutions.jts.geom._
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.adapter.raster.query.IndexOnlySpatialQuery
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
import org.apache.hadoop.mapreduce.Job
import org.apache.log4j.Logger
import org.apache.spark.Logging
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.opengis.coverage.grid.GridCoverage
import org.opengis.parameter.GeneralParameterValue

import scala.collection.JavaConverters._

import spray.json._
import spray.json.DefaultJsonProtocol._


object GeowaveAttributeStore {

  def accumuloRequiredOptions(
    zookeeper: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  ): AccumuloRequiredOptions = {
    val aro = new AccumuloRequiredOptions
    aro.setZookeeper(zookeeper)
    aro.setInstance(accumuloInstance)
    aro.setUser(accumuloUser)
    aro.setPassword(accumuloPass)
    aro.setGeowaveNamespace(geowaveNamespace)
    aro
  }

  def basicOperations(
    zookeeper: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  ): BasicAccumuloOperations = {
    return new BasicAccumuloOperations(
      zookeeper,
      accumuloInstance,
      accumuloUser,
      accumuloPass,
      geowaveNamespace)
  }

  def adapters(bao: BasicAccumuloOperations): Array[RasterDataAdapter] = {
    val as = new AccumuloAdapterStore(bao).getAdapters
    val list = Iterator.iterate(as)({ _ => as })
      .takeWhile({ _ => as.hasNext })
      .map(_.next.asInstanceOf[RasterDataAdapter])
      .toArray

    as.close
    list
  }

  def primaryIndex = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex()

  def subStrategies(idx: PrimaryIndex) = idx
    .getIndexStrategy
    .asInstanceOf[HierarchicalNumericIndexStrategy]
    .getSubStrategies

  def unapply(gas: GeowaveAttributeStore) = Some((
    gas.getBasicAccumuloOperations,
    gas.getAccumuloRequiredOptions,
    gas.getAdapters,
    gas.getPrimaryIndex,
    gas.getSubStrategies,
    gas.getBoundingBoxes
  ))
}

class GeowaveAttributeStore(
  zookeeper: String,
  accumuloInstance: String,
  accumuloUser: String,
  accumuloPass: String,
  geowaveNamespace: String
) extends DiscreteLayerAttributeStore with Logging {

  def zookeeper(): String = zookeeper
  def accumuloInstance(): String = accumuloInstance
  def accumuloUser(): String = accumuloUser
  def accumuloPass(): String = accumuloPass
  def geowaveNamespace(): String = geowaveNamespace

  val bao = GeowaveAttributeStore.basicOperations(
    zookeeper: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  )
  val aro = GeowaveAttributeStore.accumuloRequiredOptions(
    zookeeper: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  )

  val ds = new AccumuloDataStore(bao)
  val dss = new AccumuloDataStatisticsStore(bao)

  val placeHolder = "_________________________________"

  // Prevent "org.apache.accumulo.core.client.TableNotFoundException:
  // Table gwRaster_GEOWAVE_METADATA does not exist" when writing from
  // multiple threads to an initially-empty GeoWave instance.
  {
    val extent = Extent(-180, -90, 180, 90)
    val tile = IntArrayTile.empty(512, 512)
    val image = ProjectedRaster(Raster(tile, extent), LatLng).toGridCoverage2D
    val index = getPrimaryIndex
    val gwMetadata = new java.util.HashMap[String, String](); gwMetadata.put(placeHolder, placeHolder)
    val adapter = new RasterDataAdapter(placeHolder, gwMetadata, image, 256, true)
    val indexWriter = getDataStore.createWriter(adapter, index).asInstanceOf[IndexWriter[GridCoverage]]

    indexWriter.write(image); indexWriter.close
  }

  def getBoundingBoxes(): Map[ByteArrayId, BoundingBoxDataStatistics[Any]] = {
    getAdapters.map({ adapter =>
      val adapterId = adapter.getAdapterId
      val bboxId = BoundingBoxDataStatistics.STATS_ID
      val bbox = dss
        .getDataStatistics(adapterId, bboxId)
        .asInstanceOf[BoundingBoxDataStatistics[Any]]

      (adapterId, bbox)
    }).toMap
  }

  def getLeastZooms(): Map[ByteArrayId, Int] = {
    getAdapters.map({ adapter =>
      val adapterId = adapter.getAdapterId
      val bbox = getBoundingBoxes.getOrElse(adapterId, throw new Exception(s"Unknown Adapter Id $adapterId"))
      val zoom = bbox match {
        case null => {
          log.warn(s"$adapterId has a broken bounding box")
          0
        }
        case _ => {
          val substrats = getSubStrategies
          val width = bbox.getMaxX - bbox.getMinX
          val height = bbox.getMaxY - bbox.getMinY
          val zoom = (0 to getSubStrategies.length).toIterator.filter({ i =>
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

  def getAccumuloRequiredOptions = aro
  def getBasicAccumuloOperations = bao
  def getPrimaryIndex = GeowaveAttributeStore.primaryIndex
  def getAdapters = GeowaveAttributeStore.adapters(bao).filter(_.getCoverageName != placeHolder)
  def getSubStrategies = GeowaveAttributeStore.subStrategies(getPrimaryIndex)
  def getDataStore = ds
  def getDataStatisticsStore = dss

  def delete(layerId: LayerId, attributeName: String): Unit = ???
  def delete(layerId: LayerId): Unit = ???
  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] = ???
  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T = ???
  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = ???

  /**
    * Return a list of available attributes associated with this
    * attribute store.
    */
  def availableAttributes(id: LayerId) = List.empty[String]

  /**
    * Answer whether a layer exists or not.
    */
  def layerExists(layerId: LayerId): Boolean = {
    val LayerId(name, zoom) = layerId
    val candidateAdapters = getAdapters.filter(_.getCoverageName == name)

    if (candidateAdapters.nonEmpty) {
      val adapterId = candidateAdapters.head.getAdapterId
      val leastZoom = getLeastZooms.getOrElse(adapterId, throw new Exception(s"Unknown Adapter Id $adapterId"))
      ((leastZoom <= zoom) && (zoom < getSubStrategies.length))
    }
    else false
  }

  /**
    * Return a list of valid LayerIds.
    */
  def layerIds: Seq[LayerId] = {
    val list =
      for (
        adapter <- getAdapters;
        zoom <- {
          val adapterId = adapter.getAdapterId
          val leastZoom = getLeastZooms.getOrElse(adapter.getAdapterId, throw new Exception(s"Unknown Adapter Id $adapterId"))
          (leastZoom  until getSubStrategies.length)
        }
      ) yield LayerId(adapter.getCoverageName, zoom)

    list.distinct
  }
}
