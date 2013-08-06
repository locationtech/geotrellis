package geotrellis

trait SourceBuilder[Elem, +To] {
  var op:Op[Seq[Op[Elem]]] = null
  var rasterExtent:RasterExtent = null
  def result():To
}

