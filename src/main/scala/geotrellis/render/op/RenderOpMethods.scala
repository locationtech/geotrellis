package geotrellis.render.op

import geotrellis._
import geotrellis.render._
import geotrellis.source._

trait RenderOpMethods[+Repr <: RasterDS] { self: Repr =>
  def color(breaksToColors:Map[Int,Int]):RasterSource =
    color(breaksToColors,ColorMapOptions.Default)

  def color(breaksToColors:Map[Int,Int],options:ColorMapOptions):RasterSource =
    mapOp(ColorRaster(_,breaksToColors,options))

  def color(breaksToColors:Map[Double,Int])(implicit d:DI):RasterSource =
    color(breaksToColors,ColorMapOptions.Default)

  def color(breaksToColors:Map[Double,Int],options:ColorMapOptions)(implicit d:DI):RasterSource =
    mapOp(ColorRaster(_,breaksToColors,options))

  def renderPng():ValueSource[Array[Byte]] =
    renderPng(ColorRamps.BlueToRed)

  def renderPng(colorRamp:ColorRamp):ValueSource[Array[Byte]] = 
    self.converge.mapOp(SimpleRenderPng(_,colorRamp))

  def renderPng(colorBreaks:ColorBreaks):ValueSource[Array[Byte]] = 
    renderPng(colorBreaks,0)

  def renderPng(colorBreaks:ColorBreaks,noDataColor:Int):ValueSource[Array[Byte]] = 
    self.converge.mapOp(RenderPng(_,colorBreaks,noDataColor))

  def renderPng(ramp:ColorRamp, breaks:Array[Int]):ValueSource[Array[Byte]] =
    renderPng(ColorBreaks.assign(breaks,ramp.toArray))

}
