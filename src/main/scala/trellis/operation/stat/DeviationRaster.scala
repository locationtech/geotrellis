package trellis.operation

import trellis.raster.IntRaster
import trellis.stat.{Histogram, Statistics}
import trellis.process._

/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 */
case class DeviationRaster(r:Op[IntRaster], h:Op[Histogram], factor:Int) extends Op[IntRaster] {
  val g = GenerateStatistics(h)

  def childOperations = List(g, r)
  def _run(server:Server)(implicit t:Timer) = runAsync(List(g, r), server)

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
      outdata(i) = (delta * factor / stddev).asInstanceOf[Int]
      i += 1
    }
    val output = IntRaster(outdata, raster.rows, raster.cols, raster.rasterExtent.copy)
    StepResult(output)
  }
}
