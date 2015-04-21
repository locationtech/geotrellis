package geotrellis.spark.io.cassandra

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

object CassandraRasterCatalog {
  def apply()(implicit instance: CassandraInstance, sc: SparkContext): CassandraRasterCatalog = {
    val metaDataTable = ConfigFactory.load().getString("geotrellis.cassandra.catalog")
    val attributesTable = ConfigFactory.load().getString("geotrellis.cassandra.attributesCatalog")
    apply(metaDataTable, attributesTable)
  }

  def apply(metaDataTable: String, attributesTable: String)(implicit instance: CassandraInstance, sc: SparkContext): CassandraRasterCatalog =
    apply(new CassandraLayerMetaDataCatalog(instance.session, instance.keyspace, metaDataTable), CassandraAttributeStore(instance.session, instance.keyspace, attributesTable))

  def apply(metaDataCatalog: Store[LayerId, CassandraLayerMetaData], attributeStore: CassandraAttributeStore)(implicit instance: CassandraInstance, sc: SparkContext): CassandraRasterCatalog =
    new CassandraRasterCatalog(instance, metaDataCatalog, attributeStore)
}

class CassandraRasterCatalog(
  instance: CassandraInstance,
  metaDataCatalog: Store[LayerId, CassandraLayerMetaData],
  attributeStore: CassandraAttributeStore
)(implicit sc: SparkContext) {
  
  def layerMetaDataCatalog = metaDataCatalog.asInstanceOf[CassandraLayerMetaDataCatalog]

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
          CassandraLayerMetaData(
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
    val cassandraLayerMetaData = metaDataCatalog.read(layerId)
    val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
    val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
    implicitly[TileReaderProvider[K]].reader(instance, layerId, cassandraLayerMetaData, index)
  }
}
