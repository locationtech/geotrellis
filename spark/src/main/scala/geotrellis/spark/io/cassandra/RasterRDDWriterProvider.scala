package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._

import org.apache.spark.SparkContext

trait RasterRDDWriterProvider[K] {
  def writer(instance: CassandraInstance, layerMetaData: CassandraLayerMetaData, keyBounds: KeyBounds[K], keyIndex: KeyIndex[K])(implicit sc: SparkContext): RasterRDDWriter[K]
}
