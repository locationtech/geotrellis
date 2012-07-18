package geotrellis.op.raster.stat

import geotrellis.op.Op
import geotrellis.process.Context
import geotrellis.process.Result

/**
  * Generate quantile class breaks for a given raster.
  */
case class GetClassBreaks(h:Op[geotrellis.stat.Histogram], n:Op[Int]) extends Op[Array[Int]] {
  def _run(context:Context) = runAsync(List(h, n))

  val nextSteps:Steps = {
    case (histogram:geotrellis.stat.Histogram) :: (num:Int) :: Nil => step2(histogram, num)
  }

  def step2(histogram:geotrellis.stat.Histogram, num:Int) = Result(histogram.getQuantileBreaks(num))
}
