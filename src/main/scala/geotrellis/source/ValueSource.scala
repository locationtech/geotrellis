package geotrellis.source

import geotrellis._

class ValueSource[+T](val element:Op[T]) extends ValueSourceLike[T, ValueSource[T]] {
  val elements = Literal(Seq(element))
}

object ValueSource {
  def apply[T](element:Op[T]):ValueSource[T] = new ValueSource(element)
 }

trait ValueSourceLike[+T, +Repr <: ValueSource[T]] 
    extends DataSourceLike[T,T, Repr]
    with DataSource[T,T] { self: Repr => 
  def get() = self.element
}
