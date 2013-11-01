package geotrellis.source

import geotrellis._

/**
 * Bulid a data source with a single value.
 */
class ValueSourceBuilder[E:Manifest] extends SourceBuilder[E,ValueSource[E]] {
  var _dataDefinition:Op[E] = null

  def result = new ValueSource[E](_dataDefinition)

  def setOp(op: Op[Seq[Op[E]]]): this.type = {
    this._dataDefinition = op flatMap { _.apply(0) }
    this
  }
}
