package geotrellis.source

import geotrellis._

case class ValueDataSource[T:Manifest](op:Op[T]) extends DataSource[T,T] {
  val elements = Literal(Seq(op))
  def get()(implicit mf:Manifest[T]) = elements flatMap (list => list(0))
  def converge = this
}



