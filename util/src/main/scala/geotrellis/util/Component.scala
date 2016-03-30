package geotrellis.util

/** Defines an object that can be used as a lens
  * into a component C of some type T.
  */
trait Component[T, C] extends GetComponent[T, C] with SetComponent[T, C]

object Component {
  def apply[T, C](_get: T => C, _set: (T, C) => T): Component[T, C] =
    new Component[T, C] {
      val get = _get
      val set = _set
    }
}
