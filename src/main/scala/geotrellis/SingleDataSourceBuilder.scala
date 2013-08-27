package geotrellis

class SingleDataSourceBuilder[T,P](implicit converger:CanConvergeTo[T,P]) 
    extends SourceBuilder[P,SingleDataSource[T,P]] {

  var _dataDefinition:Op[Seq[Op[P]]] = null
  
  def setOp(op: Op[Seq[Op[P]]]): this.type = {
    this._dataDefinition = op
    this
  }

  def result = new SingleDataSource[T,P](_dataDefinition)
}

object SingleDataSourceBuilder {
  def apply[A](source:DataSource[A,_]) = {
    val builder = new DistributedSeqSourceBuilder[A]()
    builder
  }
}
