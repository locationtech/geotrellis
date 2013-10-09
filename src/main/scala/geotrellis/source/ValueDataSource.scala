package geotrellis.source

import geotrellis._

class ValueDataSource[+T](val element:Op[T]) extends ValueDataSourceLike[T, ValueDataSource[T]] {
  val elements = Literal(Seq(element))
}

object ValueDataSource {
  def apply[T](element:Op[T]):ValueDataSource[T] = new ValueDataSource(element)
 }

trait ValueDataSourceLike[+T, +Repr <: ValueDataSource[T]] 
    extends DataSourceLike[T,T, Repr]
    with DataSource[T,T] { self: Repr => 
  def get() = self.element
}
