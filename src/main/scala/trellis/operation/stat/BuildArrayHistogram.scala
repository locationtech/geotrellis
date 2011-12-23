package trellis.operation

import trellis.stat._


/**
  * Build an array histogram (see [[trellis.stat.ArrayHistogram]] of values from a raster.
  */
case class BuildArrayHistogram(r:IntRasterOperation, size:Int) extends BuildHistogram {
  def initHistogram = ArrayHistogram(this.size)
}
