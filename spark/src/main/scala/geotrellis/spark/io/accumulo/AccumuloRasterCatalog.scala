package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.op.stats._
import geotrellis.raster._

import org.apache.spark.SparkContext

import com.typesafe.config.{ConfigFactory,Config}

import scala.reflect._

object AccumuloRasterCatalog {
  def apply()(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog = {
    /** The value is specified in reference.conf, applications can overwrite it in their application.conf */
    val metaDataTable = ConfigFactory.load().getString("geotrellis.accumulo.catalog")
    apply(metaDataTable)
  }

  def apply(metaDataTable: String)(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog =
    apply(new AccumuloLayerMetaDataCatalog(instance.connector, metaDataTable))

  def apply(metaDataCatalog: Store[LayerId, AccumuloLayerMetaData])(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog =
    new AccumuloRasterCatalog(instance, metaDataCatalog)
}

class AccumuloRasterCatalog(instance: AccumuloInstance, metaDataCatalog: Store[LayerId, AccumuloLayerMetaData])(implicit sc: SparkContext) {
  def reader[K: RasterRDDReaderProvider](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = metaDataCatalog.read(layerId)
        implicitly[RasterRDDReaderProvider[K]].reader(instance, metaData).read(layerId, filterSet)
      }
    }
  

  def writer[K: RasterRDDWriterProvider: ClassTag](tileTable: String): Writer[LayerId, RasterRDD[K]] = {
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

        metaDataCatalog.write(layerId, md)
        implicitly[RasterRDDWriterProvider[K]].writer(instance, md).write(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def tileReader[K: TileReaderProvider](layerId: LayerId): Reader[K, Tile] = {
    val accumuloLayerMetaData = metaDataCatalog.read(layerId)
    implicitly[TileReaderProvider[K]].reader(instance, layerId, accumuloLayerMetaData)
  }
}
