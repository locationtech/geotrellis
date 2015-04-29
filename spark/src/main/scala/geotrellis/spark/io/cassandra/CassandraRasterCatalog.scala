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
  def apply()(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog = {
    val metaDataTable = ConfigFactory.load().getString("geotrellis.cassandra.catalog")
    val attributesTable = ConfigFactory.load().getString("geotrellis.cassandra.attributesCatalog")
    apply(metaDataTable, attributesTable)
  }

  def apply(metaDataTable: String, attributesTable: String)(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog =
    apply(new CassandraLayerMetaDataCatalog(metaDataTable), CassandraAttributeStore(attributesTable))

  def apply(metaDataCatalog: Store[LayerId, CassandraLayerMetaData], attributeStore: CassandraAttributeStore)(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog =
    new CassandraRasterCatalog(metaDataCatalog, attributeStore)
}

class CassandraRasterCatalog(
  metaDataCatalog: Store[LayerId, CassandraLayerMetaData],
  attributeStore: CassandraAttributeStore
)(implicit session: CassandraSession, sc: SparkContext) {
  
  def reader[K: RasterRDDReader: JsonFormat: ClassTag](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = metaDataCatalog.read(layerId)
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[RasterRDDReader[K]].read(metaData, keyBounds, index)(layerId, filterSet)
      }
    }

  def writer[K: SpatialComponent: RasterRDDWriter: JsonFormat: Ordering: ClassTag](keyIndexMethod: KeyIndexMethod[K], tileTable: String): Writer[LayerId, RasterRDD[K]] = {
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

        val rddWriter = implicitly[RasterRDDWriter[K]]
          .write(md, keyBounds, index)(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def readTile[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): K => Tile = {
    val cassandraLayerMetaData = metaDataCatalog.read(layerId)
    val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
    val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
    implicitly[TileReader[K]].read(layerId, cassandraLayerMetaData, index)(_)
  }
}
