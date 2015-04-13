package geotrellis.spark

import scala.collection.mutable

abstract sealed class KeyBound[K]

case class MinKeyBound[K]() extends KeyBound[K]
case class MaxKeyBound[K]() extends KeyBound[K]
case class ValueKeyBound[K](key: K) extends KeyBound[K]

trait KeyFilter[K] extends Serializable {
  def includeKey(key: K): Boolean
  def includePartition(minKey: KeyBound[K], maxKey: KeyBound[K]): Boolean
}
