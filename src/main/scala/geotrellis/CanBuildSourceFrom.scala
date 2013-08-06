package geotrellis

trait CanBuildSourceFrom[-From, Elem, +To] extends AnyRef {
  def apply(): SourceBuilder[Elem, To]
  //def apply[A](from: From, op:Op[Seq[Op[A]]]): SourceBuilder[Elem, To]
}
