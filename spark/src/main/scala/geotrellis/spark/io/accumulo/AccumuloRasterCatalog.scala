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

  def layerMetaDataCatalog = metaDataCatalog.asInstanceOf[AccumuloLayerMetaDataCatalog]
  
  def reader[K: RasterRDDReaderProvider: JsonFormat: ClassTag](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = metaDataCatalog.read(layerId)
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[RasterRDDReaderProvider[K]].reader(instance, metaData, keyBounds, index).read(layerId, filterSet)
      }
    }
  
  def writer[K: SpatialComponent: RasterRDDWriterProvider: JsonFormat: Ordering: ClassTag](keyIndexMethod: KeyIndexMethod[K], tileTable: String): Writer[LayerId, RasterRDD[K]] = {
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

        val minKey = rdd.map(_._1).min
        val maxKey = rdd.map(_._1).max

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

        val rddWriter = implicitly[RasterRDDWriterProvider[K]].writer(instance, md, keyBounds, index)

        rddWriter.write(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def tileReader[K: TileReaderProvider: JsonFormat: ClassTag](layerId: LayerId): Reader[K, Tile] = {
    val accumuloLayerMetaData = metaDataCatalog.read(layerId)
    val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
    val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
    implicitly[TileReaderProvider[K]].reader(instance, layerId, accumuloLayerMetaData, index)
  }
}
