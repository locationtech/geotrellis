package geotrellis.operation

import geotrellis.Raster
import geotrellis.stat.{Histogram, Statistics}
import geotrellis.process._

// TODO: rewrite this in terms of Op[Statistics].
/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 */
case class StandardDeviation(r:Op[Raster], h:Op[Histogram], factor:Int) extends Op[Raster] {
  val g = GenerateStatistics(h)

  def _run(context:Context) = runAsync(List(g, r))

  val nextSteps:Steps = {
    case (stats:Statistics) :: (raster:Raster) :: Nil => step2(stats, raster)
  }

  def step2(stats:Statistics, raster:Raster) = {
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
    val output = Raster(outdata, raster.rasterExtent)
    Result(output)
  }
}
