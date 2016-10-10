package geotrellis.raster.equalization

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.MultibandTile
import geotrellis.util.MethodExtensions


trait MultibandEqualizationMethods extends MethodExtensions[MultibandTile] {
  def equalize(histograms: Array[StreamingHistogram]): MultibandTile = HistogramEqualization(self, histograms)
  def equalize(): MultibandTile = HistogramEqualization(self)
}
