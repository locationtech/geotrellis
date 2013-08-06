package geotrellis

case class LiteralSource[T:Manifest](seqOp:Op[Seq[Op[T]]])  extends DataSource[T] with DataSourceLike[T,LiteralSource[T]] {
  def partitions():Op[Seq[Op[T]]] = seqOp 
}
