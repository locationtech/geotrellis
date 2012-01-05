package trellis.operation

import trellis.raster._
import trellis.stat._

/**
 * Build a histogram (using the [[trellis.stat.MapHistogram]] strategy) from
 * this raster.
 */
case class BuildMapHistogram(r:Op[IntRaster]) extends BuildHistogram {
  def createHistogram = MapHistogram()
}
