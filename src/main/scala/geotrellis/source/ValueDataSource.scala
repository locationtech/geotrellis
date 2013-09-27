package geotrellis.source

import geotrellis._

class ValueDataSource[T](val elements:Op[Seq[Op[T]]]) extends ValueDataSourceLike[T, ValueDataSource[T]] {
}

object ValueDataSource {
  def apply[T:Manifest](element:Op[T]):ValueDataSource[T] = new ValueDataSource(Literal(Seq(element)))
  def apply[T](elements:Op[Seq[Op[T]]]):ValueDataSource[T] = new ValueDataSource(elements)
}

trait ValueDataSourceLike[T, +Repr <: ValueDataSource[T]] 
    extends DataSourceLike[T,T, Repr]
    with DataSource[T,T] { self: Repr => 
  def get()(implicit mf:Manifest[T]) = elements flatMap (list => list(0))
}
