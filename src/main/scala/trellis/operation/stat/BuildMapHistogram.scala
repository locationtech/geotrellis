package trellis.operation

import trellis._
import trellis.process._
import trellis.stat._

/**
 * Build a histogram (using the [[trellis.stat.MapHistogram]] strategy) from
 * this raster.
 */
case class BuildMapHistogram(r:Op[IntRaster]) extends Op1(r) ({
    (raster) => {
      val histogram = FastMapHistogram()
      val data = raster.data

      var i = 0
      val len = raster.length
      while (i < len) {
        if (data(i) != NODATA) histogram.countItem(data(i), 1)
        i += 1
      }
      Result(histogram.asInstanceOf[Histogram])
    }
})
