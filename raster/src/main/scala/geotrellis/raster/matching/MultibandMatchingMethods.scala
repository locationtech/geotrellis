package geotrellis.raster.matching

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.MultibandTile
import geotrellis.util.MethodExtensions


trait MultibandMatchingMethods extends MethodExtensions[MultibandTile] {

  def matchHistogram(targetHistograms: Seq[StreamingHistogram]): MultibandTile =
    HistogramMatching(self, targetHistograms)

  def matchHistogram(
    sourceHistograms: Seq[StreamingHistogram],
    targetHistograms: Seq[StreamingHistogram]
  ): MultibandTile = HistogramMatching(self, sourceHistograms, targetHistograms)
}
