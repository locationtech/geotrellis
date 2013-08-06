package geotrellis

trait SourceBuilder[Elem, +To] {
  var op:Op[Seq[Op[Elem]]] = null
  def setOp(op:Op[Seq[Op[Elem]]]):this.type  
 // def setFrom(from:DataSource):this.type
  var rasterExtent:RasterExtent = null
  def result():To
}

