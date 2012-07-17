package geotrellis.op.stat

import geotrellis.data.{ColorBreaks => ColorBreaksObj, ColorMapper => ColorMapperObj, MultiColorRangeChooser}
import geotrellis.process._
import geotrellis.stat.{Histogram => HistogramObj}
import geotrellis._
import geotrellis.op._


case class BuildColorMapper(colorBreaks:Op[ColorBreaksObj], noDataColor:Op[Int])
extends Op2(colorBreaks, noDataColor)((bs, c) => Result(ColorMapperObj(bs, c)))

case class BuildColorBreaks(breaks:Op[Array[Int]], colors:Op[Array[Int]])
extends Op2(breaks, colors)((bs, cs) => Result(ColorBreaksObj(bs.zip(cs))))

/**
 * Generate quantile class breaks with assigned colors.
 */
case class ColorBreaks(h:Op[HistogramObj], cs:Op[Array[Int]]) extends Op[ColorBreaksObj] {

  def _run(context:Context) = runAsync(List(h, cs))

  val nextSteps:Steps = {
    case (histogram:geotrellis.stat.Histogram) :: (colors:Array[_]) :: Nil => {
      step2(histogram, colors.asInstanceOf[Array[Int]])
    }
  }

  def step2(histogram:geotrellis.stat.Histogram, colors:Array[Int]) = {
    val breaks = histogram.getQuantileBreaks(colors.length)
    Result(ColorBreaksObj(breaks.zip(colors)))
  }
}

case class ColorsFromPalette(palette:Op[Array[Int]], num:Op[Int]) extends Op2(palette, num)({
  (palette, num) => Result(new MultiColorRangeChooser(palette).getColors(num).toArray)
})
