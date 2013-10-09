package geotrellis.source

import geotrellis._

/**
 * Bulid a data source with a single value.
 */
class ValueDataSourceBuilder[E:Manifest] extends SourceBuilder[E,ValueDataSource[E]] {
  var _dataDefinition:Op[E] = null

  def result = new ValueDataSource[E](_dataDefinition)

  def setOp(op: Op[Seq[Op[E]]]): this.type = {
    this._dataDefinition = op flatMap { _.apply(0) }
    this
  }
}
