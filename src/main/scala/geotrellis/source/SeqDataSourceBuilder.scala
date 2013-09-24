package geotrellis.source

import geotrellis._

class SeqDataSourceBuilder[A] extends SourceBuilder[A,SeqDataSource[A]] {
  var _dataDefinition:Op[Seq[Op[A]]] = null

  def result = new SeqDataSource(_dataDefinition)
  
  def setOp(op: Op[Seq[Op[A]]]): this.type = {
    this._dataDefinition = op
    this
  }

}

object SeqDataSourceBuilder {
  def apply[A](source:DataSource[A,_]) = {
    val builder = new SeqDataSourceBuilder[A]()
    builder
  }
}
