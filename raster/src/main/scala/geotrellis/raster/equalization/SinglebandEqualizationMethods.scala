package geotrellis.raster.equalization

import geotrellis.raster.histogram._
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions


trait SinglebandEqualizationMethods extends MethodExtensions[Tile] {

  /**
    * Given a [[geotrellis.raster.histogram.Histogram]] which
    * summarizes this [[Tile]], equalize the histogram of this tile.
    *
    * @param  histogram  The histogram of this tile
    */
  def equalize[T <: AnyVal](histogram: Histogram[T]): Tile = HistogramEqualization(self, histogram)

  /**
    * Equalize the histogram of this [[Tile]].
    */
  def equalize(): Tile = HistogramEqualization(self)
}
