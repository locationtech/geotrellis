package geotrellis.statistics

import geotrellis._
import scala.math.{ceil, max, min, abs}
import scala.util.Sorting

import sys.error

object FastMapHistogram {
  private final val UNSET:Int = NODATA + 1
  private final val SIZE = 16

  private def buckets(size:Int) = Array.fill(size * 2)(UNSET)

  def apply() = new FastMapHistogram(SIZE, buckets(SIZE), 0, 0)
  def apply(size:Int) = new FastMapHistogram(size, buckets(size), 0, 0)

  def fromRaster(r:Raster):FastMapHistogram = {
    val h = FastMapHistogram()
    r.foreach(z => if (isData(z)) h.countItem(z, 1))
    h
  }

  def fromHistograms(hs:TraversableOnce[Histogram]):FastMapHistogram = {
    val total = FastMapHistogram()
    hs.foreach(h => total.update(h))
    total
  }
  
  /**
   * Create a histogram from double values in a raster.
   *
   * FastMapHistogram only works with integer values, which is good for performance
   * but means that, in order to use FastMapHistogram with double values, each value 
   * must be multiplied by a power of ten to preserve significant fractional digits.
   *
   * For example, if you want to save one significant digit (2.1 from 2.123), set
   * sigificantDigits to 1, and the histogram will save 2.1 as "21".
   *
   * Important: Be sure that the maximum value in the rater multiplied by 
   *            10 ^ significantDigits does not overflow Int.MaxValue (2,147,483,647). 
   */ 
  def fromRasterDouble(r:Raster, significantDigits:Int) = {
    val h = FastMapHistogram()
    val multiplier = math.pow(10, significantDigits)
    r.foreachDouble(z => if (z != Double.NaN) h.countItem( (z * multiplier).toInt, 1))
    h
  }
}

class FastMapHistogram(_size:Int, _buckets:Array[Int], _used:Int, _total:Int) 
    extends MutableHistogram {
  if (_size <= 0) error("initializeSize must be > 0")

  // we are reserving this value
  private final val UNSET:Int = NODATA + 1

  // once our buckets get 60% full, we need to resize
  private final val FACTOR:Double = 0.6

  // since the underlying implementation uses an array we can only store so
  // many unique values. 1<<30 is the largest power of two that can be
  // allocated as an array. since each key/value pair takes up two slots in our
  // array, the maximum number of those we can store is 1<<29. so that is the
  // maximum size we can allocate for our hash table.
  private final val MAXSIZE:Int = 1<<29

  // related to the hash table itself
  private var size = _size // must be a power of 2
  private var mask = size - 1
  private var limit = (size * FACTOR).toInt
  private var used = _used
  private var buckets = _buckets

  // related to the histogram
  var total = _total

  /**
   * Given item (in the range UNSET + 1 to Int.MaxValue) we return an index
   * into buckets that is either open, or allocated to item.
   *
   * The hashing strategy we use is based on Python's dictionary.
   */
  private final def hashItem(item:Int, mask:Int, bins:Array[Int]):Int = {
    var i = item & 0x7fffffff // need base hashcode to be non-negative.
    var j = (i & mask) * 2 // need result to be non-negative and even.

    // if we found our own bucket, or an empty bucket, then we're done
    var key = bins(j)
    if (key == UNSET || key == item) return j

    var failsafe = 0

    // we collided with a different item
    var perturb = i
    while (failsafe < 100000000) {
      failsafe += 1
      // i stole this whole perturbation/rehashing strategy from python
      i = (i << 2) + i + perturb + 1
      j = (i & mask) * 2

      // similarly, if we found a bucket we can use, we're done
      key = bins(j)
      if (key == UNSET || key == item) return j

      // adjust perturb. eventually perturb will zero out.
      perturb >>= 5
    }

    // should never happen
    error("should not happen: item=%s mask=%s" format (item, mask))
    -1
  }

  def setItem(item:Int, count:Int) {
    // we use our hashing strategy to figure out which bucket this item gets.
    // if the bucket is empty, we're adding the item, whereas if its not we
    // are just inreasing the item's count.
    val i = hashItem(item, mask, buckets)
    if (buckets(i) == UNSET) {
      buckets(i) = item
      buckets(i + 1) = count
      used += 1
      if (used > limit) resize()
      total += count
    } else {
      total = total - buckets(i + 1) + count
      buckets(i + 1) = count
    }
  }

  def countItem(item:Int, count:Int = 1) {
    // we use our hashing strategy to figure out which bucket this item gets.
    // if the bucket is empty, we're adding the item, whereas if its not we
    // are just inreasing the item's count.
    val i = hashItem(item, mask, buckets)
    if (buckets(i) == UNSET) {
      buckets(i) = item
      buckets(i + 1) = count
      used += 1
      if (used > limit) resize()
    } else {
      buckets(i + 1) += count
    }
    total += count
  }

  def uncountItem(item:Int) {
    // we use our hashing strategy to figure out which bucket this item gets.
    // if the bucket is empty, we never counted this item. otherwise, we need
    // to remove this value and its counts.
    val i = hashItem(item, mask, buckets)
    if (buckets(i) == UNSET) return
    total -= buckets(i + 1)
    used -= 1
    buckets(i) = UNSET
    buckets(i + 1) = UNSET
  }

  private def resize() {
    // it's important that size always be a power of 2. we grow our hash table
    // by x4 until it starts getting big, at which point we only grow by x2.
    val factor = if (size < 10000) 4 else 2

    // we build the internals of a new hash table first then rehash all our
    // existing keys/values into the new one, at which point we swap out the
    // internals.
    val nextsize = size * factor
    val nextmask = nextsize - 1
    val nextbuckets = Array.fill(nextsize * 2)(UNSET)

    // given the underlying array implementation we can only store so many
    // unique values. given that 1<<30
    if (nextsize > MAXSIZE) error("histogram has exceeded max capacity")

    var i = 0
    val len = size * 2
    while (i < len) {
      val item = buckets(i)
      if (item != UNSET) {
        val j = hashItem(item, nextmask, nextbuckets)
        nextbuckets(j) = item
        nextbuckets(j + 1) = buckets(i + 1)
      }
      i += 2
    }

    size = nextsize
    mask = nextmask
    buckets = nextbuckets
    limit = (size * FACTOR).toInt
  }

  def getTotalCount = total

  def mutable() = new FastMapHistogram(size, buckets.clone(), used, total)

  def rawValues() = {
    val keys = Array.ofDim[Int](used)
    val len = size * 2
    var i = 0
    var j = 0
    while (i < len) {
      if (buckets(i) != UNSET) {
        keys(j) = buckets(i)
        j += 1
      }
      i += 2
    }
    keys
  }

  def getValues() = {
    val keys = rawValues()
    Sorting.quickSort(keys)
    keys
  }

  def foreachValue(f:Int => Unit) {
    val len = size * 2
    var i = 0
    while (i < len) {
      if (buckets(i) != UNSET) f(buckets(i))
      i += 2
    }
  }

  def getItemCount(item:Int) = {
    val i = hashItem(item, mask, buckets)
    if (buckets(i) == UNSET) 0 else buckets(i + 1)
  }

  def getMinValue:Int = {
    val len = size * 2
    var zmin = Int.MaxValue
    var i = 0
    while (i < len) {
      val z = buckets(i)
      if (z != UNSET && z < zmin) zmin = z
      i += 2
    }
    zmin
  }

  def getMaxValue:Int = {
    val len = size * 2
    var zmax = Int.MinValue
    var i = 0
    while (i < len) {
      val z = buckets(i)
      if (z != UNSET && z > zmax) zmax = z
      i += 2
    }
    zmax
  }

  override def getMinMaxValues = {
    val len = size * 2
    var zmin = Int.MaxValue
    var zmax = Int.MinValue
    var i = 0
    while (i < len) {
      val z = buckets(i)
      if (z == UNSET) {
      } else if (z < zmin) {
        zmin = z
      } else if (z > zmax) {
        zmax = z
      }
      i += 2
    }
    // there's a small chance that we saw the max first, and zmin consumed it.
    // in that case we need to make sure to set zmax properly.
    if (zmax == Int.MinValue && zmin > zmax) zmax = zmin
    (zmin, zmax)
  }
}
