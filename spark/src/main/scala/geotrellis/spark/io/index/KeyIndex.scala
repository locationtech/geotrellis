package geotrellis.spark.io.index

import geotrellis.spark._

object KeyIndexIds {
  val hilbertSpaceTimeKeyIndex = "geotrellis.spark.io.index.hilbert.HilbertSpaceTimeKeyIndex"
  val hilbertSpatialKeyIndex   = "geotrellis.spark.io.index.hilbert.HilbertSpatialKeyIndex"
  val rowMajorSpatialKeyIndex  = "geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex"
  val zSpaceTimeKeyIndex       = "geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex"
  val zSpatialKeyIndex         = "geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex"

  val list = hilbertSpaceTimeKeyIndex :: hilbertSpatialKeyIndex ::
    rowMajorSpatialKeyIndex :: zSpaceTimeKeyIndex :: zSpatialKeyIndex :: Nil
}

trait KeyIndex[K] extends Serializable {
  val id: String = this.getClass.getName
  def toIndex(key: K): Long
  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)]
}

trait KeyIndexMethod[K, I <: KeyIndex[K]] extends Serializable {
  /** Helper method to get the resolution of a dimension. Takes the ceiling. */
  def resolution(length: Double): Int = math.ceil(scala.math.log(length) / scala.math.log(2)).toInt

  def createIndex(keyBounds: KeyBounds[K]): I
}
