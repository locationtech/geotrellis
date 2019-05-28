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

package geotrellis.layers.index

import scala.collection.TraversableOnce
import scala.collection.mutable


object MergeQueue{
  def apply(ranges: TraversableOnce[(BigInt, BigInt)]): Seq[(BigInt, BigInt)] = {
    val q = new MergeQueue()
    ranges.foreach(range => q += range)
    q.toSeq
  }
}

private class RangeComparator extends java.util.Comparator[(BigInt, BigInt)] {
  def compare(r1: (BigInt, BigInt), r2: (BigInt, BigInt)): Int = {
    val retval = (r1._2 - r2._2)
    if (retval < 0) +1
    else if (retval == 0) 0
    else -1
  }
}

class MergeQueue(initialSize: Int = 1) {
  // Sorted data structure
  private val treeSet = new java.util.TreeSet(new RangeComparator())
  // Merge near-by intervals with a gap of up to this much.  The
  // previous version of this code had an effective value of 1.
  private val gap = 1

  /**
    * Ensure that the internal array has at least `n` cells.
    */
  protected def ensureSize(n: Int): Unit = {}

  /**
    * Add a range to the merge queue.
    */
  def +=(range: (BigInt, BigInt)): Unit = treeSet.add(range)

  /**
    * Return a list of merged intervals.
    */
  def toSeq: Seq[(BigInt, BigInt)] = {
    var stack = List.empty[(BigInt, BigInt)]
    // The TreeSet will be consumed in the process below, so do not
    // use the original.
    val workingTreeSet = treeSet.clone.asInstanceOf[java.util.TreeSet[(BigInt,BigInt)]]

    if (!workingTreeSet.isEmpty) stack = (workingTreeSet.pollFirst) +: stack
    while (!workingTreeSet.isEmpty) {
      val (nextStart, nextEnd) = workingTreeSet.pollFirst
      val (currStart, currEnd) = stack.head

      // Overlap
      if (nextStart <= currStart && currStart <= nextEnd+gap) {
        // If new interval ends near the current one, extend the current one
        if (nextStart < currStart) {
          stack = (nextStart, currEnd) +: (stack.tail)
        }
      }
      else stack = (nextStart, nextEnd) +: stack
    }

    stack
  }
}
