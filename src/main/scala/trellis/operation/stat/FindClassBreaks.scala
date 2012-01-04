package trellis.operation

import trellis.process._
import trellis.stat._

/**
  * Generate quantile class breaks for a given raster.
  */
case class FindClassBreaks(h:Operation[Histogram],
                           n:Int) extends CachedOperation[Array[Int]] 
                                  with SimpleOperation[Array[Int]]{
  def _value(context:Context) = {
    val histogram = context.run(h)
    histogram.getQuantileBreaks(n)
  }
}
