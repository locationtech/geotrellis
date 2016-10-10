package geotrellis.spark.equalization

import geotrellis.raster._
import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD


trait RDDSinglebandEqualizationMethods[K, M] extends MethodExtensions[RDD[(K, Tile)] with Metadata[M]] {
  def equalize(): RDD[(K, Tile)] with Metadata[M] =
    RDDHistogramEqualization.singleband(self)

  def equalize(histogram: StreamingHistogram): RDD[(K, Tile)] with Metadata[M] =
    RDDHistogramEqualization.singleband(self, histogram)
}
