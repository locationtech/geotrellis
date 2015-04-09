package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext

trait RasterRDDReaderProvider[K] {
  def reader(catalogConfig: HadoopRasterCatalogConfig, metaData: HadoopLayerMetaData)(implicit sc: SparkContext): RasterRDDReader[K]
}
