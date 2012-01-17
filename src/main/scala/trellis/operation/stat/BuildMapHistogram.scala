package trellis.operation

import trellis.constant._
import trellis.process._
import trellis.raster._
import trellis.stat._

/**
 * Build a histogram (using the [[trellis.stat.MapHistogram]] strategy) from
 * this raster.
 */
case class BuildMapHistogram(r:Op[IntRaster]) extends SimpleOp[Histogram] {
  def _value(context:Context) = {
    val histogram = FastMapHistogram()
    val raster = context.run(r)
    val data = raster.data

    var i = 0
    val len = raster.length
    while (i < len) {
      if (data(i) != NODATA) histogram.countItem(data(i), 1)
      i += 1
    }
    histogram
  }
}
