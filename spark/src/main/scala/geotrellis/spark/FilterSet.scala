package geotrellis.spark

import scala.collection.mutable

import scala.reflect._

class FilterSet[K] extends KeyFilter[K] {
  private var _filters = mutable.ArrayBuffer[KeyFilter[K]]()

  def withFilter(filter: KeyFilter[K]) = {
    _filters += filter
    this
  }

  def filters: Seq[KeyFilter[K]] = _filters

  def isEmpty = _filters.isEmpty

  def includeKey(key: K): Boolean = {
    _filters.map(_.includeKey(key)).foldLeft(true)(_ && _)
  }

  def includePartition(minKey: KeyBound[K], maxKey: KeyBound[K]): Boolean = {
    _filters.map(_.includePartition(minKey, maxKey)).foldLeft(true)(_ && _)
  }

  def filtersWithKey[T: ClassTag]: Seq[KeyFilter[K]] =
    _filters.collect { case x: KeyFilter[T] => x }.toSeq
}

object FilterSet {
  implicit def filtersToFilterSet[K](filters: Seq[KeyFilter[K]]): FilterSet[K] =
    apply(filters)

  def empty[K] = new FilterSet[K]

  def apply[K](): FilterSet[K] = new FilterSet[K]

  def apply[K](filters: KeyFilter[K]*): FilterSet[K] =
    apply(filters)

  def apply[K](filters: Seq[KeyFilter[K]])(implicit d: DummyImplicit): FilterSet[K] = {
    val fs = new FilterSet[K]
    filters.foreach(fs.withFilter(_))
    fs
  }
}
