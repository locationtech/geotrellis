package geotrellis.statistics.op.stat

import geotrellis.data.{ColorBreaks,ColorMapper}
import geotrellis._
import geotrellis.statistics._
import geotrellis.data._

import scala.math.round

case class BuildColorMapper(colorBreaks:Op[ColorBreaks], noDataColor:Op[Int])
     extends Op2(colorBreaks, noDataColor)({
       (bs, c) => Result(ColorMapper(bs, c))
})

case class BuildColorBreaks(breaks:Op[Array[Int]], colors:Op[Array[Int]])
     extends Op2(breaks, colors)({
       (bs, cs) => Result(ColorBreaks.assign(bs, cs))
})

/**
 * Generate quantile class breaks with assigned colors.
 */
case class GetColorBreaks(h:Op[Histogram], cs:Op[Array[Int]])
     extends Op[ColorBreaks] {

  def _run(context:Context) = runAsync(List(h, cs))

  val nextSteps:Steps = {
    case (histogram:Histogram) :: (colors:Array[_]) :: Nil => {
      step2(histogram, colors.asInstanceOf[Array[Int]])
    }
  }

  def step2(histogram:Histogram, colors:Array[Int]) = {
    val limits = histogram.getQuantileBreaks(colors.length)
    Result(ColorBreaks.assign(limits, colors))
  }
}

/**
 * Creates a range of colors interpolated from a smaller set of colors.
 */
case class GetColorsFromPalette(palette:Op[Array[Int]], num:Op[Int])
     extends Op2(palette, num)({
       (palette, num) =>
         Result(new MultiColorRangeChooser(palette).getColors(num))
})
