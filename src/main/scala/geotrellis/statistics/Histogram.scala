package geotrellis.statistics

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
  def getTotalCount(): Int

  /**
   * Return the smallest item seen.
   */
  def getMinValue(): Int

  /**
   * Return the largest item seen.
   */
  def getMaxValue(): Int

  /**
   * Return the smallest and largest items seen as a tuple.
   */
  def getMinMaxValues():(Int, Int) = (getMinValue, getMaxValue)

  /**
   * Return a copy of this histogram.
   */
  def copy(): Histogram

  /**
   * This is a heuristic used by getQuantileBreaks, which mutates the
   * histogram.
   *
   *
   */
  def normalizeExtremeValues(num:Int, cutoff:Int): Histogram = {
    val (zmin, zmax) = getMinMaxValues()

    // see how many (if any) extreme values we have, and store their indices
    val values:Array[Int] = getValues()
    val vLen = values.length

    val eItems:List[Int] = values.foldLeft(Nil:List[Int]) {
      (is, i) => if (getItemCount(i) > cutoff) i :: is else is
    }
    val eLen = eItems.length

    // if we don't have extreme values we're done
    if (eLen == 0) return this

    val h = copy()

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

  def getValues():Array[Int]

  def rawValues():Array[Int]

  def foreach(f:(Int, Int) => Unit) {
    getValues.foreach(z => f(z, getItemCount(z)))
  }

  def foreachValue(f:Int => Unit): Unit

  def update(other:Histogram) {
    other.foreach((z, count) => countItem(z, count))
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
    val quantiles:Array[Double] = getEvenQuantiles(num)
    val size:Int = (quantiles(0) * getTotalCount).toInt

    // then we need to make a copy of ourself to do some preprocessing on to
    // remove extreme values. an extreme value is one that would automatically
    // overflow any bucket it is in (even alone). if size=100, and there are
    // 200 cells with the value "1" then 1 would be an extreme value.
    val h:Histogram = normalizeExtremeValues(num, size)

    // now we'll store some data about the histogram, our quantiles, etc, for
    // future use and fast access.
    val total    = h.getTotalCount
    val limits   = quantiles.map(_ * total)
    val maxValue = h.getMaxValue

    // this is the array of breaks we will return
    val breaks = Array.ofDim[Int](quantiles.length);

    // the quantile we're currently working on
    var qIndex = 0

    // the value we're currently working on
    var j = 0
    val values = getValues()

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

    // figure out which breaks got filled, and only return those
    breaks.slice(0, qIndex)
  }

  def getMode():Int = {
    if(getTotalCount == 0) { return geotrellis.NODATA }
    val values = getValues()
    var mode = values(0)
    var count = getItemCount(mode)
    var i = 1
    val len = values.length
    while (i < len) {
      val z = values(i)
      val c = getItemCount(z)
      if (c > count) {
        count = c
        mode = z
      }
      i += 1
    }
    mode
  }

  def getMedian() = if (getTotalCount == 0) {
    geotrellis.NODATA
  } else {
    val values = getValues
    values(values.length / 2)
  }

  def getMean():Double = {
    if(getTotalCount == 0) { return geotrellis.NODATA }
    val values = rawValues()
    var t = 0.0
    var i = 0
    val len = values.length
    while (i < len) {
      t += getItemCount(values(i))
      i += 1
    }
    t / getTotalCount
  }
    

  def generateStatistics() = {
    val values = getValues()

    var mode = 0
    var modeCount = 0
  
    var mean = 0.0
    var total = 0

    var median = 0
    var needMedian = true
    val limit = getTotalCount() / 2

    var i = 0
    val len = values.length;

    while (i < len) {
      val value = values(i)
      val count = getItemCount(value)
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
    val zmin = if (values.length > 0) values(0) else Int.MaxValue

    // find the max value
    val zmax = if (values.length > 0) values(len - 1) else Int.MinValue

    // find stddev
    i = 0
    total = 0
    var mean2 = 0.0
    while (i < len) {
      val value = values(i)
      val count = getItemCount(value)

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
      val count = getItemCount(values(i))
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
