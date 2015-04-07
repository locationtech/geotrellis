package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext

trait RasterRDDWriterProvider[K] {
  def writer(instance: AccumuloInstance, layerMetaData: AccumuloLayerMetaData)(implicit sc: SparkContext): RasterRDDWriter[K]
}
