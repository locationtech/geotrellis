/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
