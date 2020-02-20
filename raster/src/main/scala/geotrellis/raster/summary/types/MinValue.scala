/*
 * Copyright 2019 Azavea
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
  * Typesafe wrapper for a Double value that stores the Min value for a summary computation.
  *
  * The provided monoids ignore NaN values when combining.
  *
  * @param value
  */
case class MinValue(val value: Double) extends AnyVal {
  def toOption: Option[Double] = if (isData(value)) Some(value) else None
}

object MinValue {
  implicit val minValueMonoid: Monoid[MinValue] = new Monoid[MinValue] {
    override def empty: MinValue = MinValue(Double.NaN)

    override def combine(x: MinValue, y: MinValue): MinValue = {
      if (isData(x.value) && isData(y.value)) {
        MinValue(math.min(x.value, y.value))
      } else if (isData(x.value)) {
        x
      } else if (isData(y.value)) {
        y
      } else {
        empty
      }
    }
  }

  implicit val minValueArrayMonoid: Monoid[Array[MinValue]] = new Monoid[Array[MinValue]] {
    override def empty: Array[MinValue] = Array[MinValue]()

    override def combine(x: Array[MinValue], y: Array[MinValue]): Array[MinValue] = {
      x.zipAll(y, Monoid[MinValue].empty, Monoid[MinValue].empty).map { case (x, y) => x.combine(y) }
    }
  }
}

