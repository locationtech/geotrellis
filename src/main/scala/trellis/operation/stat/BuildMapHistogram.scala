package trellis.operation

import trellis.stat._


/**
  * Build a histogram (using the [[trellis.stat.MapHistogram]] strategy) from this raster.
  */
case class BuildMapHistogram(r:IntRasterOperation) extends BuildHistogram {
  def initHistogram = MapHistogram()
}
