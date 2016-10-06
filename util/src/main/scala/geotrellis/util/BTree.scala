package geotrellis.util

// --- //

/** An immutable Binary Tree. */
case class BTree[T](value: T, left: Option[BTree[T]], right: Option[BTree[T]]) {

  /** A generalized binary search with a custom "target test" predicate. */
  def searchWith[S](other: S, pred: (S, BTree[T]) => Either[Option[BTree[T]], T]): Option[T] = {
    pred(other, this) match {
      case Left(child) => child.flatMap(_.searchWith(other, pred))
      case Right(res)  => Some(res)
    }
  }

  /** The value of the right-most descendant node in this Tree. */
  def greatest: T = right.map(_.greatest).getOrElse(value)

  /** The value of the left-most descendant node in this Tree. */
  def lowest: T = left.map(_.lowest).getOrElse(value)

  def foreach(f: T => Unit): Unit = {
    f(value)
    left.foreach(b => f(b.value))
    right.foreach(b => f(b.value))
  }

  /* Adapted from: http://stackoverflow.com/a/8948691/643684 */
  def pretty: String = {
    def work(tree: BTree[T], prefix: String, isTail: Boolean): String = {
      val (line, bar) = if (isTail) ("└── ", " ") else ("├── ", "│")

      val curr = s"${prefix}${line}${tree.value}"

      val rights = tree.right match {
        case None    => s"${prefix}${bar}   ├── ∅"
        case Some(r) => work(r, s"${prefix}${bar}   ", false)
      }

      val lefts = tree.left match {
        case None    => s"${prefix}${bar}   └── ∅"
        case Some(l) => work(l, s"${prefix}${bar}   ", true)
      }

      s"${curr}\n${rights}\n${lefts}"

    }

    work(this, "", true)
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
