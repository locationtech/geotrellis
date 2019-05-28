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

import geotrellis.tiling._

trait KeyIndex[K] extends Serializable {
  /** Some(keybounds) if the indexed space is bounded; None if it is unbounded */
  def keyBounds: KeyBounds[K]
  def toIndex(key: K): BigInt
  def indexRanges(keyRange: (K, K)): Seq[(BigInt, BigInt)]
}

object KeyIndex {
  /**
   * Mapping KeyBounds of Extent to SFC ranges will often result in a
   * set of non-contigrious ranges.  The indices excluded by these
   * ranges should not be included in breaks calculation as they will
   * never be seen.
   */
  def breaks[K](kb: KeyBounds[K], ki: KeyIndex[K], count: Int): Vector[BigInt] = {
    breaks(ki.indexRanges(kb), count)
  }

  /**
    * Divide the space covered by ranges as evenly as possible by
    * providing break points from the ranges.  All break points will
    * be from the ranges given and never from spaces between the
    * ranges.
    *
    * @param ranges  sorted list of tuples which represent non-negative, non-intersecting ranges.
    * @param count   desired number of break points
    */
  def breaks(ranges: Seq[(BigInt, BigInt)], count: Int): Vector[BigInt] = {
    require(count > 0, "breaks count must be at least one")
    def len(r: (BigInt, BigInt)): BigInt = (r._2 - r._1) + BigInt(1)
    val total: BigInt = ranges.foldLeft(BigInt(0))(_ + len(_))
    val maxBinSize: BigInt = (total / count) + 1

    def take(range: (BigInt, BigInt), count: BigInt): BigInt = {
      if (len(range) >= count) count
      else len(range)
    }

    ranges.foldLeft((Vector.empty[BigInt], maxBinSize)) { case ((_breaks, _roomLeft), range) =>
      var breaks = _breaks
      var roomLeft = _roomLeft
      var remainder = range
      var taken = BigInt(0)
      do {
        taken = take(remainder, roomLeft)
        if (taken == roomLeft) {
          breaks = breaks :+ (remainder._1 + taken - 1)
          roomLeft = maxBinSize
          remainder = (remainder._1 + taken, remainder._2)
        } else {
          roomLeft -= taken
          remainder = (0,-1)
        }
      } while (len(remainder) > 0)
        (breaks, roomLeft)
    }._1.take(count) // we need to drop the break that falls on the end of the last range

  }
}
