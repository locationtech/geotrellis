package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics._

/**
  * Generate quantile class breaks for a given raster.
  */
case class GetClassBreaks(h:Op[Histogram], n:Op[Int]) extends Op[Array[Int]] {
  def _run() = runAsync(List(h, n))

  val nextSteps:Steps = {
    case (histogram:Histogram) :: (num:Int) :: Nil => step2(histogram, num)
  }

  def step2(histogram:Histogram, num:Int) = Result(histogram.getQuantileBreaks(num))
}
