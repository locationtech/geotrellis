package geotrellis.op.raster.stat

import geotrellis.op._
import geotrellis.process.StepOutput
import geotrellis.process.Context
import geotrellis.process.Result
import geotrellis.stat.{Statistics => StatisticsObj}
import geotrellis.Raster


// TODO: rewrite this in terms of Op[Statistics].
/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 */
case class GetStandardDeviation(r:Op[Raster], h:Op[geotrellis.stat.Histogram], factor:Int) extends Op[Raster] {
  val g = GetStatistics(h)

  def _run(context:Context) = runAsync(List(g, r))

  val nextSteps:Steps = {
    case (stats:StatisticsObj) :: (raster:Raster) :: Nil => step2(stats, raster)
  }

  def step2(stats:StatisticsObj, raster:Raster):StepOutput[Raster] = {
    val indata = raster.data.asArray.getOrElse(sys.error("need array"))
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
