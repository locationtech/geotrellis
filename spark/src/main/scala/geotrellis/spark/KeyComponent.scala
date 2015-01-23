package geotrellis.spark

import geotrellis.spark.utils._

import monocle._

trait KeyComponent[K, C] extends Serializable {
  protected def createLens(_get: K => C, _set: C => K => K): PLens[K, K, C, C] =
    PLens[K, K, C, C](_get)(_set)

  def lens: PLens[K, K, C, C]
}

trait IdentityComponent[K] extends KeyComponent[K, K] {
  def lens: PLens[K, K, K, K] =
    PLens[K, K, K, K](k => k)(k => _ => k)
}
