package geotrellis.spark.io.index

import geotrellis.spark.KeyBounds

trait BoundedKeyIndex[K] extends KeyIndex[K] {
  def keyBounds: KeyBounds[K]
}
