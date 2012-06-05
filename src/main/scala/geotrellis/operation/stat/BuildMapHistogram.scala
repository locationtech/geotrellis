package geotrellis.operation

import geotrellis._
import geotrellis.process._
import geotrellis.stat._

/**
 * Build a histogram (using the [[geotrellis.stat.MapHistogram]] strategy) from
 * this raster.
 */
case class BuildMapHistogram(r:Op[Raster]) extends Op1(r) ({
    (raster) => {
      val histogram = FastMapHistogram()
      val data = raster.data.asArray

      var i = 0
      val len = raster.length
      while (i < len) {
        if (data(i) != NODATA) histogram.countItem(data(i), 1)
        i += 1
      }
      Result(histogram.asInstanceOf[Histogram])
    }
})
