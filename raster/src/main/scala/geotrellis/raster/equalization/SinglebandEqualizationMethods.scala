package geotrellis.raster.equalization

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions


trait SinglebandEqualizationMethods extends MethodExtensions[Tile] {

  /**
    * Given a [[StreamingHistogram]] derived from this [[Tile]],
    * equalize the histogram of this tile.
    *
    * @param  histogram  The histogram of this tile
    */
  def equalize(histogram: StreamingHistogram): Tile = HistogramEqualization(self, histogram)

  /**
    * Equalize the histogram of this [[Tile]].
    */
  def equalize(): Tile = HistogramEqualization(self)
}
