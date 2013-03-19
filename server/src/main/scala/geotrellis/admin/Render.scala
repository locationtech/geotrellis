package geotrellis.admin

import geotrellis._
import geotrellis.data.ColorRamp
import geotrellis.statistics.op._

object Render {
  def apply(r:Op[Raster], ramp:Op[ColorRamp], breaks:Op[Array[Int]]) = {
    val histo = stat.GetHistogram(r)
    val colorBreaks = stat.BuildColorBreaks(breaks,ramp.map { r => r.toArray} )
    io.RenderPng(r,colorBreaks,histo,0)
  }
}
