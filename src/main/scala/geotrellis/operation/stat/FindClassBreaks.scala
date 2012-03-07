package geotrellis.operation

import geotrellis.process._
import geotrellis.stat._

/**
  * Generate quantile class breaks for a given raster.
  */
case class FindClassBreaks(h:Op[Histogram], n:Op[Int]) extends Op[Array[Int]] {
  def _run(context:Context) = runAsync(List(h, n))

  val nextSteps:Steps = {
    case (histogram:Histogram) :: (num:Int) :: Nil => step2(histogram, num)
  }

  def step2(histogram:Histogram, num:Int) = Result(histogram.getQuantileBreaks(num))
}
