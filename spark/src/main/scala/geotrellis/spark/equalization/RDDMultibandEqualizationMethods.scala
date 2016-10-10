package geotrellis.spark.equalization

import geotrellis.raster._
import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD


trait RDDMultibandEqualizationMethods[K, M] extends MethodExtensions[RDD[(K, MultibandTile)] with Metadata[M]] {
  def equalize(): RDD[(K, MultibandTile)] with Metadata[M] =
    RDDHistogramEqualization.multiband(self)

  def equalize(histograms: Array[StreamingHistogram]): RDD[(K, MultibandTile)] with Metadata[M] =
    RDDHistogramEqualization.multiband(self, histograms)
}
