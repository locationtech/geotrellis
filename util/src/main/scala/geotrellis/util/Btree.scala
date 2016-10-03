package geotrellis.util

// --- //

/** An immutable Binary Tree. */
case class BTree[T](value: T, left: Option[BTree[T]], right: Option[BTree[T]])

object BTree {
  /** Construct a balanced [[BTree]] with `O(nlogn)` time complexity.
    * '''Sortedness is not checked.'''
    */
  def fromSortedSeq[T: Ordering](items: IndexedSeq[T]): Option[BTree[T]] = {
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
