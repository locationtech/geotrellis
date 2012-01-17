package trellis.operation

import trellis.data.ColorBreaks
import trellis.process._
import trellis.stat._

/**
 * Generate quantile class breaks with assigned colors.
 */
case class FindColorBreaks(h:Op[Histogram], cs:Op[Array[Int]]) extends Op[ColorBreaks] {

  def _run(context:Context) = runAsync(List(h, cs))

  val nextSteps:Steps = {
    case (histogram:Histogram) :: (colors:Array[_]) :: Nil => {
      step2(histogram, colors.asInstanceOf[Array[Int]])
    }
  }

  def step2(histogram:Histogram, colors:Array[Int]) = {
    val breaks = histogram.getQuantileBreaks(colors.length)
    Result(ColorBreaks(breaks.zip(colors)))
  }
}
