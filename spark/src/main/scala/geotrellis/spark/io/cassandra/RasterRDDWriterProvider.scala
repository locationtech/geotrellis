package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext

trait RasterRDDWriterProvider[K] {
  def writer(instance: CassandraInstance, layerMetaData: CassandraLayerMetaData)(implicit sc: SparkContext): RasterRDDWriter[K]
}
