package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics._


/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 *
 * @return        Raster of TypeInt data
 *
 * @note          Currently only supports working with integer types. If you pass in a Raster
 *                with double type data (TypeFloat,TypeDouble) the values will be rounded to
 *                Ints.
 */
case class GetStandardDeviation(r:Op[Raster], h:Op[Histogram], factor:Int) extends Op[Raster] {
  val g = GetStatistics(h)

  def _run() = runAsync(List(g, r))

  val nextSteps:Steps = {
    case (stats:Statistics) :: (raster:Raster) :: Nil => step2(stats, raster)
  }

  def step2(stats:Statistics, raster:Raster):StepOutput[Raster] = {
    val indata = raster.toArray
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
