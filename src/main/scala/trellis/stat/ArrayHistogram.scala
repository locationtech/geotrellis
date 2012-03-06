package geotrellis.stat

import math.{abs, ceil, min, max, sqrt}

object ArrayHistogram {
  def apply(size:Int) = new ArrayHistogram(Array.fill[Int](size)(0), 0)

  def apply(counts:Array[Int], total:Int) = new ArrayHistogram(counts, total)
}

// TODO: can currently only handle non-negative integers

/**
  * Data object representing a histogram that uses an array for internal storage. 
  */
class ArrayHistogram(val counts:Array[Int], var total:Int) extends Histogram {
  def getTotalCount = this.total

  def copy = ArrayHistogram(this.counts.clone, this.total)

  //def getValues = (0 until this.counts.length).toArray
  def getValues = (0 until this.counts.length).filter(this.counts(_) > 0).toArray

  def uncountItem(i:Int) {
    this.total -= counts(i)
    this.counts(i) = 0
  }

  def countItem(i:Int, count:Int=1) {
    this.total += count
    this.counts(i) += count
  }

  def getItemCount(i:Int) = this.counts(i)

  // REFACTOR: use Option
  def getMinValue:Int = {
    var i = 0
    val limit = this.counts.length
    while (i < limit) {
      if (this.counts(i) > 0) return i
      i += 1
    }
    return Int.MaxValue
  }

  // REFACTOR: use Option
  def getMaxValue:Int = {
    var i = this.counts.length - 1
    while (i >= 0) {
      if (this.counts(i) > 0) return i
      i -= 1
    }
    return Int.MinValue
  }
}
