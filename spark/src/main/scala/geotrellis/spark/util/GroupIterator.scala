package geotrellis.spark.util

/**
  * Groups consecutive records that share a projection from an iterator to form an iterator of iterators.
  *
  * You can think of this as Iterator equivalent of.groupBy that only works on consecutive records.
  */
class GroupConsecutiveIterator[T, R](iter: Iterator[T])(f: T => R)
    extends Iterator[(R, Iterator[T])] {
  private var remaining = iter.buffered

  def hasNext = remaining.hasNext

  def next = {
    val cur = f(remaining.head)
    val (left, right) = remaining.span(x => f(x) == cur)
    remaining = right.buffered
    (cur, left)
  }
}

object GroupConsecutiveIterator {
  def apply[T, R](iter: Iterator[T])(f: T => R) =
    new GroupConsecutiveIterator(iter)(f)
}
