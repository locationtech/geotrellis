package geotrellis.spark.io.index

import geotrellis.spark.io.index.hilbert.{HilbertSpatialKeyIndex, HilbertSpaceTimeKeyIndex}
import geotrellis.spark.io.index.zcurve.{ZSpatialKeyIndex, ZSpaceTimeKeyIndex}
import geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex

import scala.reflect.{ClassTag, classTag}

trait KeyIndex[K] extends Serializable {
  val id: String = this.getClass.getName
  def toIndex(key: K): Long
  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)]
}

object KeyIndex {
  private def getName[T: ClassTag] = classTag[T].toString

  val hilbertSpaceTimeKeyIndex = getName[HilbertSpaceTimeKeyIndex]
  val hilbertSpatialKeyIndex   = getName[HilbertSpatialKeyIndex]
  val rowMajorSpatialKeyIndex  = getName[RowMajorSpatialKeyIndex]
  val zSpaceTimeKeyIndex       = getName[ZSpaceTimeKeyIndex]
  val zSpatialKeyIndex         = getName[ZSpatialKeyIndex]

  val list = hilbertSpaceTimeKeyIndex :: hilbertSpatialKeyIndex ::
    rowMajorSpatialKeyIndex :: zSpaceTimeKeyIndex :: zSpatialKeyIndex :: Nil
}

trait KeyIndexMethod[K] extends Serializable {
  /** Helper method to get the resolution of a dimension. Takes the ceiling. */
  def resolution(length: Double): Int = math.ceil(scala.math.log(length) / scala.math.log(2)).toInt + 1

  def createIndex(keyBounds: KeyBounds[K]): KeyIndex[K]
}
