package trellis.operation

import trellis.process._
import trellis.stat._

// TODO: parallelize
/**
  * Generate quantile class breaks for a given raster.
  */
case class FindClassBreaks(h:Op[Histogram], n:Op[Int]) extends SimpleOp[Array[Int]] {
  def _value(context:Context) = {
    val histogram = context.run(h)
    val num = context.run(n)
    histogram.getQuantileBreaks(num)
  }
}
