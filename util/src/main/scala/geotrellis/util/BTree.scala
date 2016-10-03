package geotrellis.util

// --- //

/** An immutable Binary Tree. */
case class BTree[T](value: T, left: Option[BTree[T]], right: Option[BTree[T]]) {

  /** A generalized binary search with a custom "target test" predicate. */
  def searchWith(pred: BTree[T] => Either[Option[BTree[T]], T]): Option[T] = {
    pred(this) match {
      case Left(child) => child.flatMap(_.searchWith(pred))
      case Right(res)  => Some(res)
    }
  }

  def foreach(f: T => Unit): Unit = {
    f(value)
    left.foreach(b => f(b.value))
    right.foreach(b => f(b.value))
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
