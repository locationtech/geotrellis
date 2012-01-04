package trellis.operation.render.png

import trellis.data.ColorBreaks
import trellis.process._
import trellis.operation._

/**
  * Generate a PNG from a given raster and a set of color breaks.  The background
  * can be set to a color or be made transparent.
  */
case class RenderPNG2(r:IntRasterOperation, c:Operation[ColorBreaks],
                      noDataColor:Int, transparent:Boolean)
extends PNGOperation with SimpleOperation[Array[Byte]] {
  def _value(context:Context) = {
    val colorBreaks = context.run(c)
    val p = RenderPNG(r, colorBreaks.breaks, noDataColor, transparent)
    context.run(p)
  }
}

