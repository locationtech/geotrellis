package geotrellis.spark

import geotrellis.spark.utils._

import monocle._
import monocle.syntax._

trait KeyComponent[K, C] extends Serializable {
  protected def createLens(_get: K => C, _set: C => K => K): PLens[K, K, C, C] =
    PLens[K, K, C, C](_get)(_set)

  def lens: PLens[K, K, C, C]
}

trait IdentityComponent[K] extends KeyComponent[K, K] {
  def lens: PLens[K, K, K, K] =
    PLens[K, K, K, K](k => k)(k => _ => k)
}


trait Component[C, T] extends Serializable {
  protected def createLens(_get: T => C, _set: C => T => T): PLens[T, T, C, C] =
    PLens[T, T, C, C](_get)(_set)

  def lens: PLens[T, T, C, C]
}

object Component {
  def get[T, C](target: T)(implicit c: Component[C, T]) =
    target &|-> c.lens get

  def set[T, C](target: T, component: C)(implicit c: Component[C, T]) =
    target &|-> c.lens set(component)
}
