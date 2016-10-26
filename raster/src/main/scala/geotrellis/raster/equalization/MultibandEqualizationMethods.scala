package geotrellis.raster.equalization

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.MultibandTile
import geotrellis.util.MethodExtensions


trait MultibandEqualizationMethods extends MethodExtensions[MultibandTile] {

  /**
    * Equalize the histograms of the bands of this [[MultibandTile]].
    *
    * @param  histogram  A sequence of [[StreamingHistogram]] objects, one for each band
    * @return            A multiband tile whose bands have equalized histograms
    */
  def equalize(histograms: Array[StreamingHistogram]): MultibandTile = HistogramEqualization(self, histograms)

  /**
    * Equalize the histograms of the bands of this [[MultibandTile]].
    *
    * @return  A multiband tile whose bands have equalized histograms
    */
  def equalize(): MultibandTile = HistogramEqualization(self)
}
