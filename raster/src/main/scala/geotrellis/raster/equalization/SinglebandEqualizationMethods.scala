package geotrellis.raster.equalization

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions


trait SinglebandEqualizationMethods extends MethodExtensions[Tile] {
  def equalize(histogram: StreamingHistogram): Tile = HistogramEqualization(self, histogram)
  def equalize(): Tile = HistogramEqualization(self)
}
