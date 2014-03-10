package geotrellis.render.op

import geotrellis._
import geotrellis.render._
import geotrellis.statistics.op.stat

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
object SimpleRenderPng {
  def apply(r:Op[Raster],colors:Op[Array[Int]])(implicit d:DI):Op[Png] = 
    r.flatMap { r =>
        stat.GetHistogram(r).flatMap { h =>
          val colorBreaks = GetColorBreaks(h, colors)
          RenderPng(r,colorBreaks,0,h)
        }
      }
     .withName("SimpleRenderPng")

  def apply(r:Op[Raster],colorRamp:Op[ColorRamp] = ColorRamps.HeatmapBlueToYellowToRedSpectrum):Op[Png] = 
    apply(r,colorRamp.map(_.toArray))
}
