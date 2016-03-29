package geotrellis.util

trait SetComponent[T, C] extends Serializable {
  def set: (T, C) => T
}

object SetComponent {
  def apply[T, C](_set: (T, C) => T): SetComponent[T, C] =
    new SetComponent[T, C] {
      val set = _set
    }
}
