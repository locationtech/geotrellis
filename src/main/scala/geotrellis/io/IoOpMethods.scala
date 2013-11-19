package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.source._

trait IoOpMethods[+Repr <: RasterSource] { self: Repr =>
  def renderPng(colorRamp:ColorRamp) = 
    self.converge.mapOp(SimpleRenderPng(_,colorRamp))

  def renderPng(colorBreaks:ColorBreaks):ValueSource[Array[Byte]] = 
    renderPng(colorBreaks,0)

  def renderPng(colorBreaks:ColorBreaks,noDataColor:Int):ValueSource[Array[Byte]] = 
    self.converge.mapOp(RenderPng(_,colorBreaks,noDataColor))

  def writePng(path:String) = 
    self.converge.mapOp(WritePngRgba(_,path))
}
