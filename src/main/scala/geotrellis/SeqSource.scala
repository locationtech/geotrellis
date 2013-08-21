package geotrellis

trait SeqSource[A] extends DataSource[Seq[A]] {
  var _dataDefinition:Op[Seq[Op[Seq[A]]]] = null

  def setOp(op:Op[Seq[Op[Seq[A]]]]):this.type = {
    this._dataDefinition = op
    this
  }
}
