package trellis.operation

import trellis.process._
import trellis.stat._

/**
  * Generate quantile class breaks for a given raster.
  */
case class FindClassBreaks(h:Operation[Histogram],
                           n:Int) extends CachedOperation[Array[Int]] 
                                  with SimpleOperation[Array[Int]]{
  def childOperations = { List(h) }
  def _value(server:Server)(implicit t:Timer) = {
    val histogram = server.run(h)
    histogram.getQuantileBreaks(n)
  }
}
