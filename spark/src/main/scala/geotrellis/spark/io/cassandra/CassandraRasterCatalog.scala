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
    val attributesTable = ConfigFactory.load().getString("geotrellis.cassandra.attributesCatalog")
    apply(attributesTable)
  }

  def apply(attributesTable: String)(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog =
    apply(CassandraAttributeStore(attributesTable))

  def apply(attributeStore: CassandraAttributeStore)(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog =
    new CassandraRasterCatalog(attributeStore)
}

class CassandraRasterCatalog(
  attributeStore: CassandraAttributeStore
)(implicit session: CassandraSession, sc: SparkContext) {

  def reader[K: RasterRDDReader: JsonFormat: ClassTag](): FilterableRasterRDDReader[K] =
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = attributeStore.read[CassandraLayerMetaData](layerId, "metadata")
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[RasterRDDReader[K]].read(metaData, keyBounds, index)(layerId, filterSet)
      }
    }

  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: Ordering: ClassTag](keyIndexMethod: KeyIndexMethod[K], tileTable: String): Writer[LayerId, RasterRDD[K]] = {
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        // Persist since we are both calculating a histogram and saving tiles.
        rdd.persist()

        val md =
          CassandraLayerMetaData(
            rasterMetaData = rdd.metaData,
            keyClass = classTag[K].toString,
            tileTable = tileTable
          )

        val keyBounds = rdd.keyBounds

        val index = {
          val indexKeyBounds = {
            val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        attributeStore.write(layerId, "metadata", md)
        attributeStore.write[KeyBounds[K]](layerId, "keyBounds", keyBounds)
        attributeStore.write(layerId, "keyIndex", index)

        val rddWriter =
          implicitly[RasterRDDWriter[K]]
            .write(md, keyBounds, index)(layerId, rdd)

        rdd.unpersist(blocking = false)
      }
    }
  }

  def readTile[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): K => Tile = {
    val cassandraLayerMetaData = attributeStore.read[CassandraLayerMetaData](layerId, "metadata")
    val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
    val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
    implicitly[TileReader[K]].read(layerId, cassandraLayerMetaData, index)(_)
  }
}
