package trellis.operation

import trellis.data.ColorBreaks
import trellis.process._
import trellis.stat._

/**
  * Generate quantile class breaks with assigned colors.
  */
case class FindColorBreaks(h:Operation[Histogram], n:Int,
                           colors:Array[Int]) extends Operation[ColorBreaks] 
                                              with SimpleOperation[ColorBreaks] {
  def _value(context:Context) = {
    val histogram = context.run(h)
    val breaks = histogram.getQuantileBreaks(n)
    ColorBreaks(breaks.zip(colors))
  }
}
