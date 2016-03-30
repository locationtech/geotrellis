package geotrellis.util

trait GetComponent[T, C] extends Serializable {
  def get: T => C
}

object GetComponent {
  def apply[T, C](_get: T => C): GetComponent[T, C] =
    new GetComponent[T, C] {
      val get = _get
    }
}
