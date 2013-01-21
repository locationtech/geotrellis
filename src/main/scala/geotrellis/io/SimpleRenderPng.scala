package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.statistics.op._
import geotrellis.statistics.Histogram

/**
 * Generate a PNG image from a data raster.
 *
 * This operation is designed to provide a simple interface to generate a
 * colored image from a data raster.  The data values in your raster will
 * be classified into a number of ranges, and cells in each range will be
 * rendered with a unique color.  You can select the number of ranges that
 * will be used, and the color ramp from which the colors will be selected.
 *
 * There are some color ramps you can select in geotrellis.data, and the
 * default ramp (if you do not provide one) ranges from red to yellow to green.
 *
 * @param r   Raster to vizualize as an image
 * @param colorRamp   Colors to select from
 */
case class SimpleRenderPng(r: Op[Raster], colorRamp: Op[ColorRamp] = ColorRamps.HeatmapBlueToYellowToRedSpectrum)
  extends Op[Array[Byte]] {
  def _run(context: Context) = runAsync('step1 :: r :: Nil)
  val nextSteps: Steps = {
    case 'step1 :: (r: Raster) :: Nil => step2(r)
    case 'step2 :: (h: Histogram) :: (r: Raster) :: (c: ColorRamp) :: Nil => step3(h, r, c)
    case 'result :: (bytes: Array[_]) :: Nil => Result(bytes.asInstanceOf[Array[Byte]])
  }
  def step2(r: Raster) = {
    val histogramOp = stat.GetHistogram(r)
    runAsync('step2 :: histogramOp :: r :: colorRamp :: Nil)
  }
  def step3(histogram: Histogram, r: Raster, colorRamp: ColorRamp) = {
    val breaksOp = stat.GetColorBreaks(histogram, colorRamp.colors)
    val renderOp = io.RenderPng(r, breaksOp, histogram, 0)
    runAsync('result :: renderOp :: Nil)
  }
}


