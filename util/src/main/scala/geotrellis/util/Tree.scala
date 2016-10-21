package geotrellis.util

// --- //

/** A typical immutable Tree implementation, mysteriously absent from
  * Scala's standard library.
  */
case class Tree[T](root: T, children: Seq[Tree[T]]) {
  /** The elements of the tree in pre-order. */
  def flatten: Seq[T] = root +: children.flatMap(_.flatten)
}

object Tree {
  /** Construct a Tree with a single node and no children. */
  def singleton[T](t: T): Tree[T] = Tree(t, Seq.empty[Tree[T]])

  implicit class TreeFunctor[T](val self: Tree[T]) extends Functor[Tree, T] {
    def map[S](f: T => S): Tree[S] = Tree(
      f(self.root),
      self.children.map(t => t.map(f))
    )
  }
}
