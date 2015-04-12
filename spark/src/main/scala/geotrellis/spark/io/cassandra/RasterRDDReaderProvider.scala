package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext

trait RasterRDDReaderProvider[K] {
  def reader(instance: CassandraInstance, metaData: CassandraLayerMetaData, keyBounds: KeyBounds[K])(implicit sc: SparkContext): FilterableRasterRDDReader[K]
}
