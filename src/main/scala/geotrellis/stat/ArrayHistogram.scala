package geotrellis.stat

import geotrellis._
import math.{abs, ceil, min, max, sqrt}

object ArrayHistogram {
  def apply(size:Int) = new ArrayHistogram(Array.fill[Int](size)(0), 0)

  def apply(counts:Array[Int], total:Int) = new ArrayHistogram(counts, total)

  def fromRaster(r:Raster, n:Int) = {
    val h = ArrayHistogram(n)
    r.foreach(z => if (z != NODATA) h.countItem(z, 1))
    h
  }

  def fromHistograms(hs:List[Histogram], n:Int) = {
    val total:Histogram = ArrayHistogram(n)
    hs.foreach(h => total.update(h))
    total
  }
}

// TODO: can currently only handle non-negative integers

/**
  * Data object representing a histogram that uses an array for internal storage. 
  */
class ArrayHistogram(val counts:Array[Int], var total:Int) extends Histogram {
  def size = counts.length

  def getTotalCount = total

  def copy = ArrayHistogram(counts.clone, total)

  //def getValues = (0 until counts.length).toArray
  def getValues = (0 until counts.length).filter(counts(_) > 0).toArray

  def setItem(i:Int, count:Int) {
    total = total - counts(i) + count
    counts(i) = count
  }

  def uncountItem(i:Int) {
    total -= counts(i)
    counts(i) = 0
  }

  def countItem(i:Int, count:Int=1) {
    total += count
    counts(i) += count
  }

  def getItemCount(i:Int) = counts(i)

  // REFACTOR: use Option
  def getMinValue:Int = {
    var i = 0
    val limit = counts.length
    while (i < limit) {
      if (counts(i) > 0) return i
      i += 1
    }
    return Int.MaxValue
  }

  // REFACTOR: use Option
  def getMaxValue:Int = {
    var i = counts.length - 1
    while (i >= 0) {
      if (counts(i) > 0) return i
      i -= 1
    }
    return Int.MinValue
  }
}
