package geotrellis.spark.io.index

import geotrellis.spark._

trait KeyIndexMethod[K] extends Serializable {
  /** Helper method to get the resolution of a dimension. Takes the ceiling. */
  def resolution(length: Double): Int = math.ceil(scala.math.log(length) / scala.math.log(2)).toInt

  // RETODO: Rename to createKeyIndex? toKeyIndex?
  def createIndex(keyBounds: KeyBounds[K]): KeyIndex[K]
}
