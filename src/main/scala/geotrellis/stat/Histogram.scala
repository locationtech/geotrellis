package geotrellis.stat

import math.{abs, round, sqrt}

/**
  * Data object representing a histogram of values.
  */
abstract trait Histogram {
  /**
   * Note the occurance of 'item''.
   *
   * The optional parameter 'count' allows histograms to be built more
   * efficiently. Negative counts can be used to remove a particular number
   * of occurances of 'item'.
   */
  def countItem(item:Int, count:Int = 1): Unit

  /**
   * Forget all occurances of 'item'.
   */
  def uncountItem(item:Int): Unit

  /**
   * 
   */
  def setItem(item:Int, count:Int): Unit

  /**
   * Return the number of occurances for 'item'.
   */
  def getItemCount(item:Int): Int

  /**
   * Return the total number of occurances for all items.
   */
  def getTotalCount: Int

  /**
   * Return the smallest item seen.
   */
  def getMinValue: Int

  /**
   * Return the largest item seen.
   */
  def getMaxValue: Int

  /**
   * Return the smallest and largest items seen as a tuple.
   */
  def getMinMaxValues:(Int, Int) = (getMinValue, getMaxValue)

  /**
   * Return a copy of this histogram.
   */
  def copy: Histogram

  /**
   * This is a heuristic used by getQuantileBreaks, which mutates the
   * histogram.
   *
   *
   */
  def normalizeExtremeValues(num:Int, cutoff:Int): Histogram = {
    val (zmin, zmax) = getMinMaxValues

    // see how many (if any) extreme values we have, and store their indices
    val values:Array[Int] = getValues
    val vLen:Int = values.length

    val eItems:List[Int] = values.foldLeft(Nil:List[Int]) {
      (is, i) => if (getItemCount(i) > cutoff) i :: is else is
    }
    val eLen:Int = eItems.length

    // if we don't have extreme values we're done
    if (eLen == 0) return this

    val h = this.copy

    // if we only have extreme values, just set all histogram counts to 1.
    if (eLen == vLen) {
      eItems.foreach(item => h.setItem(item, 1))
      return h
    }

    // ok, so we want extreme values to each get exactly one bucket after
    // normalization. we will assign each of our extreme indices the same
    // value, which will be our new target bucket size. to do this, we have to
    // take into account the "new total" (consisting of all our non-extreme
    // values plus the "new" extreme values). here is an equation that might
    // help get the idea across:
    //
    // T: the total of all "non-extreme" values added together
    // Q: the number of quantiles we want
    // E: the number of extreme values we have
    // X: our goal, an extreme value which we will assign into the histogram
    //    for the extreme indices which *also* will correspond to our new
    //    bucket size
    //
    // X             = (T + E * X) / Q
    // X * Q         = (T + E * X)
    // X * Q - X * E = T
    // X * (Q - E)   = T
    // X             = T / (Q - E)
    val eSubtotal:Int = eItems.foldLeft(0)((t, i) => t + h.getItemCount(i))
    val oSubtotal:Int = h.getTotalCount - eSubtotal
    var eValue:Int = oSubtotal / (num - eLen)

    eItems.foreach(i => h.setItem(i, eValue))
    h
  }

  def getValues:Array[Int]

  /**
   * The basic idea here is to try to "evenly" split 'b' values across 'q'
   * slots. The goal is to drop or duplicate values as evenly as possible.
   * Here are some examples:
   *
   * splitBreakIndices(1, 1) -> Array(0)
   * splitBreakIndices(3, 5) -> Array(1,2,4)
   * splitBreakIndices(4, 5) -> Array(0,1,3,4)
   * splitBreakIndices(5, 5) -> Array(0,1,2,3,4)
   * splitBreakIndices(7, 5) -> Array(0,1,1,2,3,3,4)
   * splitBreakIndices(10,5) -> Array(0,0,1,1,2,2,3,3,4,4)
   */
  def splitBreakIndices(b:Int, q:Int):Array[Int] = {
    val ratio = q.toDouble / b
    (0 until b).map(i => scala.math.round(i * ratio).toInt).toArray
  }

  /**
   * Take an existing array of class breaks (values that define the maximum for
   * a particular class) and "stretch" them over a larger range.
   * The idea is that a user might want 6 classes, but only 4 could be created.
   * In these cases we will streth the 4 breaks into 6 (with some breaks being
   * unused) to better map onto a users' preferred colors.
   */
  def splitBreaks(breaks:Array[Int], q:Int):Array[Int] = {
    val indices = splitBreakIndices(breaks.length, q)
    var out = Array.ofDim[Int](q)

    var i = 0
    var j = 0

    while (i < q) {
      if (j < indices.length - 1 && indices(j + 1) == i) j += 1
      out(i) = breaks(j)
      i += 1
    }

    out
  }

  /**
   * Return 'num' evenly spaced Doubles from 0.0 to 1.0.
   */
  def getEvenQuantiles(num:Int) = (1 to num).map(_.toDouble / num).toArray

  /**
   *
   */
  def getQuantileBreaks(num:Int):Array[Int] = {
    // first, we create a list of percentages to use, along with determining
    // how many cells should fit in one "ideal" quantile bucket.
    val quantiles = this.getEvenQuantiles(num)
    val size = (quantiles(0) * this.getTotalCount).toInt

    // then we need to make a copy of ourself to do some preprocessing on to
    // remove extreme values. an extreme value is one that would automatically
    // overflow any bucket it is in (even alone). if size=100, and there are
    // 200 cells with the value "1" then 1 would be an extreme value.
    val h = normalizeExtremeValues(num, size)

    // now we'll store some data about the histogram, our quantiles, etc, for
    // future use and fast access.
    val total    = h.getTotalCount
    val limits   = quantiles.map { q => q * total }.toArray
    val maxValue = h.getMaxValue

    // this is the array of breaks we will return
    val breaks = Array.ofDim[Int](quantiles.length);

    // the quantile we're currently working on
    var qIndex = 0

    // the value we're currently working on
    var j = 0
    val t0 = System.currentTimeMillis()
    val values = this.getValues
    val t1 = System.currentTimeMillis()

    // the current total of all previous values we've seen
    var currTotal = 0

    // we're going to move incrementally through the values while comparing
    // a running total against our current quantile (qIndex). we know that the
    // last break is "everything else" so we stop when we reach that one.
    while (qIndex < breaks.length && j < values.length) {
      val i = values(j)
      val count = h.getItemCount(i)
      val newTotal = currTotal + count

      if (count == 0) {
      } else if (newTotal > limits(qIndex)) {
        if (abs(limits(qIndex) - currTotal) > abs(limits(qIndex) - newTotal)) {
          // in this case values(j) is closer than values(j - 1)
          breaks(qIndex) = i
        } else if(j > 0) {
          // in this case values(j - 1) is closer, did we already use it?
          if (qIndex > 0 && breaks(qIndex - 1) == values(j - 1)) {
            // yes, so now use values(j)
            breaks(qIndex) = values(j)
          } else {
            // no, so use values(j - 1)
            breaks(qIndex) = values(j - 1)
          }
        } else {
          // in this case j == 0 so there is no values(j - 1)
          breaks(qIndex) = i
        }

        qIndex += 1
      }

      currTotal = newTotal
      j += 1
    }

    // put the maximum value at the end
    if (qIndex < breaks.length && (qIndex == 0 || breaks(qIndex - 1) < maxValue)) {
      breaks(qIndex) = maxValue
      qIndex += 1
    }

    // figure out which breaks got filled
    val realBreaks = breaks.slice(0, qIndex)
    splitBreaks(realBreaks, quantiles.length)
  }

  def generateStatistics() = {
    val values = this.getValues

    var mode = 0
    var modeCount = 0
  
    var mean = 0.0
    var total = 0

    var median = 0
    var needMedian = true
    val limit = this.getTotalCount / 2

    var i = 0
    val len = values.length;

    while (i < len) {
      val value = values(i)
      val count = this.getItemCount(value)
      if (count != 0) {
        // update the mode
        if (count > modeCount) {
          mode = value
          modeCount = count
        }
  
        // update the mean
        val delta = value - mean
        total += count
        mean += (count * delta) / total

        // update median if needed
        if (needMedian && total > limit) {
          median = values(i)
          needMedian = false
        }
      }
      i += 1
    }

    // find the min value
    val zmin = if (values.length > 0) values(0) else 0

    // find the max value
    val zmax = if (values.length > 0) values(len - 1) else 0

    // find stddev
    i = 0
    total = 0
    var mean2 = 0.0
    while (i < len) {
      val value = values(i)
      val count = this.getItemCount(value)

      if (count > 0) {
        val x = value - mean
        val y = x * x
  
        val delta = y - mean2
        total += count
        mean2 += (count * delta) / total
      }

      i += 1
    }
    val stddev = sqrt(mean2)

    Statistics(mean, median, mode, stddev, zmin, zmax)
  }

  // TODO: use a standard json library
  def toJSON = {
    val sb = new StringBuilder()
    sb.append("[")
    var first = true
    val values = getValues
    var i = 0
    while (i < values.length) {
      val count = this.getItemCount(values(i))
      if (count > 0) {
        if (first) first = false else sb.append(",")
        sb.append("[%d,%d]".format(values(i), count))
      }
      i += 1
    }
    sb.append("]")
    sb.toString()
  }
}
