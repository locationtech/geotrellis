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
