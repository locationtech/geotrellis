package geotrellis.operation

import geotrellis.data._
import geotrellis.process._
import geotrellis.stat._

case class BuildColorMapper(colorBreaks:Op[ColorBreaks], noDataColor:Op[Int])
extends Op2(colorBreaks, noDataColor)((bs, c) => Result(ColorMapper(bs, c)))

case class BuildColorBreaks(breaks:Op[Array[Int]], colors:Op[Array[Int]])
extends Op2(breaks, colors)((bs, cs) => Result(ColorBreaks(bs.zip(cs))))

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

case class ColorsFromPalette(palette:Op[Array[Int]], num:Op[Int]) extends Op2(palette, num)({
  (palette, num) => Result(new MultiColorRangeChooser(palette).getColors(num).toArray)
})
