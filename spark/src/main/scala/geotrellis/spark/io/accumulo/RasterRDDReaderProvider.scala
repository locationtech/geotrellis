package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext

trait RasterRDDReaderProvider[K] {
  def reader(instance: AccumuloInstance, metaData: AccumuloLayerMetaData)(implicit sc: SparkContext): FilterableRasterRDDReader[K]
}
