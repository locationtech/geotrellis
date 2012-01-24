package trellis.operation


import trellis._
import trellis.process._
import trellis.stat._

/**
 * Build an array histogram (see [[trellis.stat.ArrayHistogram]] of values from
 * a raster.
 */
case class BuildArrayHistogram(r:Op[IntRaster], size:Op[Int]) extends Op2(r,size) ({
  (raster, size) => {
    val histogram = ArrayHistogram(size)

    val data = raster.data

    var i = 0
    val len = data.length
    while (i < len) {
      val z = data(i)
      if (z != NODATA && z >= 0) histogram.countItem(z, 1)
      i += 1
    }
    Result(histogram.asInstanceOf[Histogram])
  }
})
