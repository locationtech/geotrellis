package geotrellis.op.stat

import geotrellis._
import geotrellis.op._
import geotrellis.process._
import geotrellis.stat._

/**
  * Generate quantile class breaks for a given raster.
  */
case class ClassBreaks(h:Op[geotrellis.stat.Histogram], n:Op[Int]) extends Op[Array[Int]] {
  def _run(context:Context) = runAsync(List(h, n))

  val nextSteps:Steps = {
    case (histogram:geotrellis.stat.Histogram) :: (num:Int) :: Nil => step2(histogram, num)
  }

  def step2(histogram:geotrellis.stat.Histogram, num:Int) = Result(histogram.getQuantileBreaks(num))
}
