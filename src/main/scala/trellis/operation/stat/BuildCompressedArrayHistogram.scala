package trellis.operation

import trellis.stat._

/**
  * Build a histogram (using the [[trellis.stat.CompressedArrayHistogram]] strategy) from this raster.
  */
case class BuildCompressedArrayHistogram(r:IntRasterOperation, vmin:Int, vmax:Int,
                                         size:Int) extends BuildHistogram {
  def initHistogram = CompressedArrayHistogram(vmin, vmax, size)
}
