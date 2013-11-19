package geotrellis.admin

import geotrellis._
import geotrellis.data.ColorRamp
import geotrellis.statistics.op._

object Render {
  def apply(r:Op[Raster], ramp:Op[ColorRamp], breaks:Op[Array[Int]]) = {
    val colorBreaks = stat.BuildColorBreaks(breaks,ramp.map(_.toArray))
    io.RenderPng(r,colorBreaks,0)
  }
}
