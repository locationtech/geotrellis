package geotrellis.spark.io.index

import geotrellis.spark.KeyBounds

trait KeyIndex[K] extends Serializable {
  /** Some(keybounds) if the indexed space is bounded; None if it is unbounded */
  def keyBounds: KeyBounds[K]
  def toIndex(key: K): Long
  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)]
}
