package geotrellis.source

import geotrellis.RasterExtent
import geotrellis._

trait SourceBuilder[Elem, +To] {
  var op:Op[Seq[Op[Elem]]] = null
  def setOp(op:Op[Seq[Op[Elem]]]):this.type  
  var rasterExtent:RasterExtent = null
  def result():To
}

