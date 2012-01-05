package trellis.operation

import trellis.stat._
import trellis.raster._

/**
 * Build an array histogram (see [[trellis.stat.ArrayHistogram]] of values from
 * a raster.
 */
case class BuildArrayHistogram(r:Op[IntRaster], size:Int)
extends BuildHistogram {
  def createHistogram = ArrayHistogram(size)
}
