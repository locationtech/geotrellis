package geotrellis.spark.equalization

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD


trait RDDSinglebandEqualizationMethods[K, M] extends MethodExtensions[RDD[(K, Tile)] with Metadata[M]] {

  /**
    * Equalize the histograms of the respective Tile objects in the
    * RDD using one joint histogram derived from the entire source
    * RDD.
    */
  def equalize(): RDD[(K, Tile)] with Metadata[M] =
    RDDHistogramEqualization.singleband(self)

  /**
    * Given a histogram derived form the source RDD of Tile objects,
    * equalize the respective histograms of the RDD of tiles.
    *
    * @param  histogram  A histogram
    */
  def equalize[T <: AnyVal](histogram: Histogram[T]): RDD[(K, Tile)] with Metadata[M] =
    RDDHistogramEqualization.singleband(self, histogram)
}
