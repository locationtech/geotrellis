package geotrellis.statistics

import geotrellis._
import scala.collection.mutable.Map
import scala.math.{max, min}
import scala.util.Sorting

object MapHistogram {
  def apply() = new MapHistogram(Map.empty[Int, Int], 0)

  def apply(counts:Map[Int, Int], total:Int) = new MapHistogram(counts, total)
}

/**
  * Data object representing a histogram that uses a Map (as in hashtable map/dictionary) for internal storage.
  */
class MapHistogram(counts:Map[Int, Int], var total:Int) extends Histogram {
  def getTotalCount = this.total

  def copy() = MapHistogram(this.counts.clone, this.total)

  def getValues() = {
    var keys = rawValues()
    Sorting.quickSort(keys)
    keys
  }

  def rawValues() = counts.keys.toArray

  def foreachValue(f:Int => Unit) {
    counts.keys.foreach(f)
  }

  def uncountItem(i:Int) {
    if (i != NODATA) {
      val opt = this.counts.remove(i)
      if (opt.isDefined) {
        this.total -= opt.get
      }
    }
  }

  def countItem(i:Int, count:Int=1) {
    if (i != NODATA) {
      if (this.counts.contains(i)) {
        this.counts(i) += count
      } else {
        this.counts(i) = count
      }
      this.total += count
    }
  }

  def setItem(i:Int, count:Int) {
    if (i != NODATA) {
      if (this.counts.contains(i)) {
        this.total = this.total + count - this.counts(i)
        this.counts(i) = count
      } else {
        this.counts(i) = count
        this.total += count
      }
    }
  }

  def getItemCount(i:Int) = this.counts.getOrElse(i, 0)

  //def getMinValue:Int = this.counts.keys.foldLeft(Int.MaxValue)(min)
  //def getMaxValue:Int = this.counts.keys.foldLeft(Int.MinValue)(max)

  def getMinValue = getMinMaxValues._1
  def getMaxValue = getMinMaxValues._2

  override def getMinMaxValues = {
    val it = counts.keys.toIterator
    if (it.isEmpty) {
      (Int.MaxValue, Int.MinValue)
    } else {
      var zmin = it.next()
      var zmax = zmin
      for (z <- it) {
        if (z < zmin) {
          zmin = z
        } else if (z > zmax) {
          zmax = z
        }
      }
      (zmin, zmax)
    }
  }
}
