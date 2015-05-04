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

object AccumuloRasterCatalog {
  def apply()(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog = {
    /** The value is specified in reference.conf, applications can overwrite it in their application.conf */
    val attributesTable = ConfigFactory.load().getString("geotrellis.accumulo.catalog")
    apply(attributesTable)
  }

  def apply(attributesTable: String)(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog =
     new AccumuloRasterCatalog(instance, AccumuloAttributeStore(instance.connector, attributesTable))
}

class AccumuloRasterCatalog(
  instance: AccumuloInstance, 
  attributeStore: AccumuloAttributeStore
)(implicit sc: SparkContext) {
  def reader[K: RasterRDDReader: JsonFormat: ClassTag](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = attributeStore.read[AccumuloLayerMetaData](layerId, "metadata")
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[RasterRDDReader[K]].read(instance, metaData, keyBounds, index)(layerId, filterSet)
      }
    }
  
  def writer[K: SpatialComponent: RasterRDDWriter: JsonFormat: Ordering: ClassTag](keyIndexMethod: KeyIndexMethod[K], tileTable: String): Writer[LayerId, RasterRDD[K]] = {
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        // Persist since we are both calculating a histogram and saving tiles.
        rdd.persist()

        val md = 
          AccumuloLayerMetaData(
            layerId = layerId,
            rasterMetaData = rdd.metaData,            
            keyClass = classTag[K].toString,
            tileTable = tileTable
          )

        val minKey = rdd.map(_._1).min
        val maxKey = rdd.map(_._1).max

        val keyBounds = KeyBounds(minKey, maxKey)

        val index = {
          val indexKeyBounds = {
            val imin = minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        attributeStore.write(layerId, "metadata", md)      
        attributeStore.write(layerId, "keyBounds", keyBounds)
        attributeStore.write(layerId, "keyIndex", index)

        val rddWriter = implicitly[RasterRDDWriter[K]]
          .write(instance, md, keyBounds, index)(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def readTile[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): K => Tile = {
    val accumuloLayerMetaData = attributeStore.read[AccumuloLayerMetaData](layerId, "metadata")
    val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
    val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
    implicitly[TileReader[K]].read(instance, layerId, accumuloLayerMetaData, index)(_)
  }
}
