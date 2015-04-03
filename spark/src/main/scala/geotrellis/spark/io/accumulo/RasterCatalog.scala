package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.op.stats._
import geotrellis.raster._

import org.apache.spark.SparkContext

import scala.reflect._

object RasterCatalog {
  def apply(instance: AccumuloInstance, metaDataTable: String)(implicit sc: SparkContext): RasterCatalog =
    new RasterCatalog(instance, new AccumuloLayerMetaDataCatalog(instance.connector, metaDataTable))
}

class RasterCatalog(instance: AccumuloInstance, metaDataCatalog: Store[LayerId, AccumuloLayerMetaData])(implicit sc: SparkContext) {
  def reader[K: RasterRDDReaderProvider](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = metaDataCatalog.read(layerId)
        implicitly[RasterRDDReaderProvider[K]].reader(instance, metaData).read(layerId, filterSet)
      }
    }
  

  def writer[K: RasterRDDWriterProvider: ClassTag](tileTable: String): Writer[LayerId, RasterRDD[K]] = {
    val rddWriter = implicitly[RasterRDDWriterProvider[K]].writer(instance, metaDataCatalog, tileTable)

    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        rdd.persist()
        val layerMetaData = LayerMetaData(
          rasterMetaData = rdd.metaData,
          keyClass = classTag[K].toString,
          histogram = Some(rdd.histogram)
        )

        val md = AccumuloLayerMetaData(layerMetaData, tileTable)

        metaDataCatalog.write(layerId, md)
        rddWriter.write(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def tileReader[K: TileReaderProvider](layerId: LayerId): Reader[K, Tile] = {
    val accumuloLayerMetaData = metaDataCatalog.read(layerId)
    implicitly[TileReaderProvider[K]].reader(instance, layerId, accumuloLayerMetaData)
  }
}
