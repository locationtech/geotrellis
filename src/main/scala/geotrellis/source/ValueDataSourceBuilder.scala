package geotrellis.source

import geotrellis._

/**
 * Bulid a data source with a single value.
 */
class ValueDataSourceBuilder[E] extends SourceBuilder[E,ValueDataSource[E]] {
  var _dataDefinition:Op[Seq[Op[E]]] = null

  def result = new ValueDataSource[E](_dataDefinition)

  def setOp(op: Op[Seq[Op[E]]]): this.type = {
    this._dataDefinition = op
    this
  }
}
