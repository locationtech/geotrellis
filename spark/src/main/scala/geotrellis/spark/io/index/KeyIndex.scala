package geotrellis.spark.io.index

import geotrellis.spark.KeyBounds

trait KeyIndex[K] extends Serializable {
  /** Some(keybounds) if the indexed space is bounded; None if it is unbounded */
  def keyBounds: KeyBounds[K]
  def toIndex(key: K): Long
  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)]
}

object KeyIndex {
  /**
   * Mapping KeyBounds of Extent to SFC ranges will often result in a set of non-contigrious ranges.
   * The indices excluded by these ranges should not be included in breaks calculation as they will never be seen.
   */
  def breaks[K](kb: KeyBounds[K], ki: KeyIndex[K], count: Int): Vector[Long] = {
    breaks(ki.indexRanges(kb), count)
  }

  /**
    * Divide the space covered by ranges as evenly as possible by providing break points from the ranges.
    * All break points will be from the ranges given and never from spaces between the ranges.
    *
    * @param ranges  sorted list of tuples which represent non-negative, non-intersecting ranges.
    * @param count   desired number of break points
    */
  def breaks(ranges: Seq[(Long, Long)], count: Int): Vector[Long] = {
    require(count > 0, "breaks count must be at least one")
    def len(r: (Long, Long)) = r._2 - r._1 + 1l
    val total: Double = ranges.foldLeft(0l)(_ + len(_))
    val maxBinSize =  math.max(math.ceil(total / (count+1)), 1).toLong

    def take(range: (Long, Long), count: Long): Long = {
      if (len(range) >= count) count
      else len(range)
    }
    ranges.foldLeft((Vector.empty[Long], maxBinSize)) { case ((_breaks, _roomLeft), range) =>
      var breaks = _breaks
      var roomLeft = _roomLeft
      var remainder = range
      var taken: Long = 0l
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
