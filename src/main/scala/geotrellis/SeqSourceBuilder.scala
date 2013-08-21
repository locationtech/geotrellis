package geotrellis

trait SeqSourceBuilder[A,Repr] extends SourceBuilder[A,Repr] {
  var _dataDefinition:Op[Seq[Op[A]]] = null
  
  def setOp(op: Op[Seq[Op[A]]]): this.type = {
    this._dataDefinition = op
    this
  }
}



