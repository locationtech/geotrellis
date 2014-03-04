/*******************************************************************************
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
  *
  */
class MapHistogram(counts:Map[Int, Int], var total:Int) extends MutableHistogram {
  def getTotalCount = this.total

  def mutable() = MapHistogram(this.counts.clone, this.total)

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
    if (isData(i)) {
      val opt = this.counts.remove(i)
      if (opt.isDefined) {
        this.total -= opt.get
      }
    }
  }

  def countItem(i:Int, count:Int=1) {
    if (isData(i)) {
      if (this.counts.contains(i)) {
        this.counts(i) += count
      } else {
        this.counts(i) = count
      }
      this.total += count
    }
  }

  def setItem(i:Int, count:Int) {
    if (isData(i)) {
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
