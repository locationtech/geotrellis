package geotrellis.spark.equalization

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD


trait RDDMultibandEqualizationMethods[K, M] extends MethodExtensions[RDD[(K, MultibandTile)] with Metadata[M]] {

  /**
    * Equalize the histograms of the respective bands of the RDD of
    * MultibandTile objects using one joint histogram derived from
    * each band of the input RDD.
    */
  def equalize(): RDD[(K, MultibandTile)] with Metadata[M] =
    RDDHistogramEqualization.multiband(self)

  /**
    * Given a sequence of Histogram objects (one per band), equalize
    * the histograms of the respective bands of the RDD of
    * MultibandTile objects.
    *
    * @param  histograms  A sequence of histograms
    */
  def equalize[T <: AnyVal](histograms: Array[Histogram[T]]): RDD[(K, MultibandTile)] with Metadata[M] =
    RDDHistogramEqualization.multiband(self, histograms)
}
