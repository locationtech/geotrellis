package geotrellis

trait SourceBuilder[Elem, +To] {
  var op:Op[Seq[Op[Elem]]] = null
  def setOp(op:Seq[Op[Elem]]):this.type
  def setDataDefinition(dfn:Op[DataDefinition]):this.type
  
  var rasterExtent:RasterExtent = null
  def result():To
}

