package geotrellis.spark

import monocle._

trait KeyComponent[K, C] extends Serializable {
  protected def createLens(_get: K => C, _set: C => K => K): Lens[K, C] =
    Lens[K, C](_get)(_set)

  def lens: Lens[K, C]
}

trait IdentityComponent[K] extends KeyComponent[K, K] {
  def lens: Lens[K, K] =
    Lens[K, K](k => k)(k => _ => k)
}
