package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.op.stats._
import geotrellis.raster._

import org.apache.spark.SparkContext

import com.typesafe.config.{ConfigFactory,Config}

import scala.reflect._
import spray.json._

object CassandraRasterCatalog {
  def apply()(implicit instance: CassandraInstance, sc: SparkContext): CassandraRasterCatalog = {
    val metaDataTable = ConfigFactory.load().getString("geotrellis.cassandra.catalog")
    apply(metaDataTable)
  }

  def apply(metaDataTable: String)(implicit instance: CassandraInstance, sc: SparkContext): CassandraRasterCatalog =
    apply(metaDataTable, metaDataTable)

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
  def reader[K: RasterRDDReaderProvider: JsonFormat](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = metaDataCatalog.read(layerId)
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        implicitly[RasterRDDReaderProvider[K]].reader(instance, metaData, keyBounds).read(layerId, filterSet)
      }
    }

  def writer[K: RasterRDDWriterProvider: JsonFormat: Ordering: ClassTag](tileTable: String): Writer[LayerId, RasterRDD[K]] = {
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
        attributeStore.write[KeyBounds[K]](layerId, "keyBounds", KeyBounds(minKey, maxKey))
        implicitly[RasterRDDWriterProvider[K]].writer(instance, md).write(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def tileReader[K: TileReaderProvider](layerId: LayerId): Reader[K, Tile] = {
    val cassandraLayerMetaData = metaDataCatalog.read(layerId)
    implicitly[TileReaderProvider[K]].reader(instance, layerId, cassandraLayerMetaData)
  }
}
