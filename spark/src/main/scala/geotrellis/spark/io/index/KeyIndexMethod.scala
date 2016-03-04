package geotrellis.spark.io.index

import geotrellis.spark._

trait KeyIndexMethod[K] extends Serializable {
  /** Helper method to get the resolution of a dimension. Takes the ceiling. */
  def resolution(max: Double, min: Double): Int = {
    val length = max - min + 1
    math.ceil(scala.math.log(length) / scala.math.log(2)).toInt
  }

  def createIndex(keyBounds: KeyBounds[K]): KeyIndex[K]
}
