package geotrellis.op.stat

import geotrellis._
import geotrellis.op._
import geotrellis.stat._

/**
 * Build a histogram (using the [[geotrellis.stat.CompressedArrayHistogram]]
 * strategy) from this raster.
 */
case class BuildCompressedArrayHistogram(r:Op[Raster], vmin:Int, vmax:Int,
                                         size:Int) extends BuildHistogram {
  def createHistogram = CompressedArrayHistogram(vmin, vmax, size)
}
