package geotrellis.admin

import geotrellis._
import geotrellis.render._
import geotrellis.render.op._
import geotrellis.statistics.op._

object Render {
  def apply(r:Op[Raster], ramp:Op[ColorRamp], breaks:Op[Array[Int]]) = {
    val colorBreaks = BuildColorBreaks(breaks,ramp.map(_.toArray))
    RenderPng(r,colorBreaks,0)
  }
}
