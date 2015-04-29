package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.op.stats._
import geotrellis.raster._

import org.apache.spark.SparkContext

import com.typesafe.config.{ConfigFactory,Config}

import scala.reflect._
import spray.json._

import scala.math.Ordering.Implicits._

object AccumuloRasterCatalog {
  def apply()(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog = {
    /** The value is specified in reference.conf, applications can overwrite it in their application.conf */
    val metaDataTable = ConfigFactory.load().getString("geotrellis.accumulo.catalog")
    apply(metaDataTable)
  }

  def apply(metaDataTable: String)(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog =
    apply(metaDataTable, metaDataTable)

  def apply(metaDataTable: String, attributesTable: String)(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog =
    apply(new AccumuloLayerMetaDataCatalog(instance.connector, metaDataTable), AccumuloAttributeStore(instance.connector, attributesTable))

  def apply(metaDataCatalog: Store[LayerId, AccumuloLayerMetaData], attributeStore: AccumuloAttributeStore)(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog =
    new AccumuloRasterCatalog(instance, metaDataCatalog, attributeStore)
}

class AccumuloRasterCatalog(
  instance: AccumuloInstance, 
  metaDataCatalog: Store[LayerId, AccumuloLayerMetaData],
  attributeStore: AccumuloAttributeStore
)(implicit sc: SparkContext) {
  def reader[K: RasterRDDReader: JsonFormat: ClassTag](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = metaDataCatalog.read(layerId)
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[RasterRDDReader[K]].read(instance, metaData, keyBounds, index)(layerId, filterSet)
      }
    }
  def writer[K: SpatialComponent: RasterRDDWriter: JsonFormat: Ordering: ClassTag](keyIndexMethod: KeyIndexMethod[K], tileTable: String): Writer[LayerId, RasterRDD[K]] = {

    def keyMinMax(rdd: RasterRDD[K]): (K, K) = {
        val initialKey: K = rdd.map(_._1).first
        val aggregateInit = (initialKey, initialKey)
        def merger(t: (K, K), v: K): (K, K) = {
          if (t._1 > v) (v, t._2)
          else if (t._2 < v) (t._1, v)
          else t
        }

        def combiner(t1: (K, K), t2: (K, K)): (K, K) = {
          (t1._1 min t2._1, t1._2 max t2._2)
        }

        rdd.map(_._1).aggregate(aggregateInit)(merger, combiner)
    }

    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        // Persist since we are both calculating a histogram and saving tiles.
        rdd.persist()

        val md = 
          AccumuloLayerMetaData(
            rasterMetaData = rdd.metaData,
            histogram = Some(rdd.histogram),
            keyClass = classTag[K].toString,
            tileTable = tileTable
          )

        val (minKey, maxKey) = keyMinMax(rdd)

        metaDataCatalog.write(layerId, md)
        val keyBounds = KeyBounds(minKey, maxKey)
        attributeStore.write[KeyBounds[K]](layerId, "keyBounds", keyBounds)

        val index = {
          val indexKeyBounds = {
            val imin = minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }
        attributeStore.write(layerId, "keyIndex", index)

        val rddWriter = implicitly[RasterRDDWriter[K]]
          .write(instance, md, keyBounds, index)(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def readTile[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): K => Tile = {
    val accumuloLayerMetaData = metaDataCatalog.read(layerId)
    val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
    val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
    implicitly[TileReader[K]].read(instance, layerId, accumuloLayerMetaData, index)(_)
  }
}
