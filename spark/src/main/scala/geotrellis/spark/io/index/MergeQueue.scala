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

package geotrellis.spark.io.index

import scala.collection.TraversableOnce
import scala.collection.mutable


object MergeQueue{
  def apply(ranges: TraversableOnce[(Long, Long)]): Seq[(Long, Long)] = {
    val q = new MergeQueue()
    ranges.foreach(range => q += range)
    q.toSeq
  }
}

private class RangeComparator extends java.util.Comparator[(Long, Long)] {

  def compare(r1: (Long, Long), r2: (Long, Long)): Int = {
    val retval = (r1._1 - r2._1)
    if (retval < 0) -1
    else if (retval == 0) 0
    else +1
  }
}

class MergeQueue(initialSize: Int = 1) {
  private val treeSet = new java.util.TreeSet(new RangeComparator()) // Sorted data structure
  private val fudge = 1 // Set this to one to preserve original behavior

  /**
    * Ensure that the internal array has at least `n` cells.
    */
  protected def ensureSize(n: Int): Unit = {}

  /**
    * Add a range to the merge queue.
    */
  def +=(range: (Long, Long)): Unit = treeSet.add(range)

  /**
    * Return a list of merged intervals.
    */
  def toSeq: Seq[(Long, Long)] = {
    var stack = List.empty[(Long, Long)]

    if (!treeSet.isEmpty) stack = (treeSet.pollFirst) +: stack
    while (!treeSet.isEmpty) {
      val (nextStart, nextEnd) = treeSet.pollFirst
      val (currStart, currEnd) = stack.head

      // Overlap
      if ((nextStart <= currStart && currStart <= nextEnd) || (nextStart <= (currEnd+fudge) && currEnd <= nextEnd)) {
        // If new interval ends after the current one, extend the current one
        if (currEnd < nextEnd) {
          stack = (currStart, nextEnd) +: (stack.tail)
        }
      }
      else stack = (nextStart, nextEnd) +: stack
    }

    stack.reverse
  }

}
