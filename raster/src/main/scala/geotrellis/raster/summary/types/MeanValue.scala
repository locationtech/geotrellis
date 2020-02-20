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

package geotrellis.raster.summary.types

import cats.Monoid
import cats.syntax.monoid._
import geotrellis.raster._

/**
  * Immutable class that computes a derived mean from stored sum and count
  *
  * In order to iteratively compute a mean, use the overridden + operator
  * to "add" additional values into the result.
  *
  * This class handles NaN sum by setting the count to zero. This effectively
  * skips any NaN values in the mean calculation.
  *
  * @param sum
  * @param count
  */
class MeanValue(val sum: Double, val count: Long) extends Serializable {
  def mean: Double = if (count < 1) {
    Double.NaN
  } else {
    sum / count
  }

  def +(b: MeanValue) = MeanValue(sum + b.sum, count + b.count)
}
object MeanValue {

  def apply(sum: Double, count: Long): MeanValue = {
    if (isNoData(sum) || count < 1) {
      new MeanValue(0, 0L)
    } else {
      new MeanValue(sum, count)
    }
  }

  implicit val meanValueMonoid: Monoid[MeanValue] = new Monoid[MeanValue] {
    override def empty: MeanValue = MeanValue(0, 0L)

    override def combine(x: MeanValue, y: MeanValue): MeanValue = x + y
  }

  implicit val meanValueArrayMonoid: Monoid[Array[MeanValue]] = new Monoid[Array[MeanValue]] {
    override def empty: Array[MeanValue] = Array[MeanValue]()

    override def combine(x: Array[MeanValue], y: Array[MeanValue]): Array[MeanValue] = {
      x.zipAll(y, Monoid[MeanValue].empty, Monoid[MeanValue].empty).map { case (x, y) => x.combine(y) }
    }
  }
  
  def fromFullTile(tile: Tile): MeanValue = {
    var s = 0
    var c = 0L
    tile.foreach((x: Int) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanValue(s, c)
  }

  def fromFullTileDouble(tile: Tile): MeanValue = {
    var s = 0.0
    var c = 0L
    tile.foreachDouble((x: Double) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanValue(s, c)
  }
}

