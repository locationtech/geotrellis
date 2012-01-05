package trellis.operation

import trellis.raster._
import trellis.stat._

/**
 * Build a histogram (using the [[trellis.stat.CompressedArrayHistogram]]
 * strategy) from this raster.
 */
case class BuildCompressedArrayHistogram(r:Op[IntRaster], vmin:Int, vmax:Int, size:Int)
extends BuildHistogram {
  def createHistogram = CompressedArrayHistogram(vmin, vmax, size)
}
