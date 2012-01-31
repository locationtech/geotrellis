package trellis.operation

import trellis.IntRaster
import trellis.stat.{Histogram, Statistics}
import trellis.process._

// TODO: rewrite this in terms of Op[Statistics].
/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 */
case class StandardDeviation(r:Op[IntRaster], h:Op[Histogram], factor:Int) extends Op[IntRaster] {
  val g = GenerateStatistics(h)

  def _run(context:Context) = runAsync(List(g, r))

  val nextSteps:Steps = {
    case (stats:Statistics) :: (raster:IntRaster) :: Nil => step2(stats, raster)
  }

  def step2(stats:Statistics, raster:IntRaster) = {
    val indata = raster.data
    val len = indata.length
    val outdata = Array.ofDim[Int](len)

    val mean = stats.mean
    val stddev = stats.stddev

    var i = 0
    while (i < len) {
      val delta = indata(i) - mean
      outdata(i) = (delta * factor / stddev).toInt
      i += 1
    }
    val output = IntRaster(outdata, raster.rows, raster.cols, raster.rasterExtent.copy)
    Result(output)
  }
}
