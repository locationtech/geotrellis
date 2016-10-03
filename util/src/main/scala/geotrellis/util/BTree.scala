package geotrellis.util

// --- //

/** An immutable Binary Tree. */
case class BTree[T](value: T, left: Option[BTree[T]], right: Option[BTree[T]]) {

  /** A generalized binary search with a custom "target test" predicate. */
  def searchWith[S](
    other: S,
    pred: (S, T, Option[BTree[T]], Option[BTree[T]]) => Ord
  ): Option[T] = {
    pred(other, value, left, right) match {
      case EQ => Some(value)
      case LT => left.flatMap(_.searchWith(other, pred))
      case _  => right.flatMap(_.searchWith(other, pred))
    }
  }
}

object BTree {
  /** Construct a balanced [[BTree]] with `O(nlogn)` time complexity.
    * '''Sortedness is not checked.'''
    */
  def fromSortedSeq[T](items: IndexedSeq[T]): Option[BTree[T]] = {
    if (items.isEmpty) {
      None
    } else {
      val m: Int = items.length / 2

      Some(BTree(
        items(m),
        fromSortedSeq(items.slice(0, m)),
        fromSortedSeq(items.slice(m + 1, items.length))
      ))
    }
  }
}
