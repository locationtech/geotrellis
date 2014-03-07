package geotrellis.source

import geotrellis._

/**
 * Bulid a data source with a converging function. 
 */
case class DataSourceBuilder[E,V](convergeFunction:(Op[Seq[Op[E]]]) => Op[V]) extends SourceBuilder[E,DataSource[E,V]] {
  var _dataDefinition:Op[Seq[Op[E]]] = null

  def result = new DataSource[E,V] {
    val elements = _dataDefinition
    def convergeOp() = convergeFunction(elements)
  }

  def setOp(op: Op[Seq[Op[E]]]): this.type = {
    this._dataDefinition = op
    this
  }
}
