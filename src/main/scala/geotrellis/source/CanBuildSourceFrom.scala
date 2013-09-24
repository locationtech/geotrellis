package geotrellis.source

trait CanBuildSourceFrom[-From, Elem, +To] extends AnyRef {
  def apply(): SourceBuilder[Elem, To]
  def apply(from: From): SourceBuilder[Elem, To]
}
