package trellis.stat

import math.{abs, sqrt}

/**
  * Data object representing a histogram of values.
  */
abstract trait Histogram {
  def countItem(i:Int, count:Int=1)
  def uncountItem(i:Int)

  def getItemCount(i:Int): Int
  def getTotalCount: Int

  def getMinValue: Int
  def getMaxValue: Int
  def getMinMaxValues:(Int, Int) = (getMinValue, getMaxValue)

  def copy: Histogram

  def getEvenQuantiles(num:Int) = {
    val d = 1.0 / num
    (1 to num).map { x => x * d }.toArray
  }

  def normalizeExtremeValues(num:Int, cutoff:Int) {
    val (zmin, zmax) = getMinMaxValues

    // see how many (if any) extreme values we have, and store their indices
    val values = this.getValues
    val eIndices = values.foldLeft(List[Int]()) {
      (L, i) => if(this.getItemCount(i) > cutoff) i :: L else L
    }

    // if we don't have exreme values we're done
    if (eIndices.length == 0) { return }

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
    val eSubtotal = eIndices.foldLeft(0) { (t, i) => t + this.getItemCount(i) }
    val oSubtotal = this.getTotalCount - eSubtotal
    val eValue    = oSubtotal / (num - eIndices.length)

    eIndices.foreach {
      i => {
        this.uncountItem(i)
        this.countItem(i, eValue)
      }
    }
  }

  def getValues:Array[Int]

  // TODO: only use the 'nearst strategy
  def getQuantileBreaks(num:Int):Array[Int] = {
    // first, we create a list of percentages to use, along with determining
    // how many cells should fit in one "ideal" quantile bucket.
    val quantiles = getEvenQuantiles(num)
    val size = (quantiles(0) * this.getTotalCount).toInt

    // then we need to make a copy of ourself to do some preprocessing on to
    // remove extreme values. an extreme value is one that would automatically
    // overflow any bucket it is in (even alone). if size=100, and there are
    // 200 cells with the value "1" then 1 would be an extreme value.
    val h = this.copy
    h.normalizeExtremeValues(num, size)

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
    //val values = this.getValues()
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
    if (qIndex < breaks.length) {
      breaks(qIndex) = maxValue
      qIndex += 1
    }

    // return the breaks, but only the ones we filled
    breaks.slice(0, qIndex)
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

  // TODO: remove this
  def sparseString = {
    var result = "{"
    val (zmin, zmax) = getMinMaxValues
    //val zmin = this.getMinValue
    //val zmax = this.getMaxValue
    for(i <- zmin to zmax) {
      val count = this.getItemCount(i)
      if (count > 0) {
        result += "\n %d:%d".format(i, count)
      }
    }
    if (result.length > 1) {
      result + "\n}"
    } else {
      "{}"
    }
  }

  // TODO: use a standard json library
  def toJSON = {
    val sb = new StringBuilder()
    sb.append("[")
    
    //val (zmin, zmax) = getMinMaxValues
    //
    //var first = true
    //var i = zmin
    //
    //while (i <= zmax) {
    //  val count = this.getItemCount(i)
    //  if (count > 0) {
    //    if (first) { first = false } else { sb.append(",") }
    //    sb.append("[%d,%d]".format(i, count))
    //  }
    //  i += 1
    //}
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
