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

package geotrellis.raster.summary.polygonal.visitors

import cats.Monoid
import cats.syntax.monoid._
import geotrellis.raster._

trait Implicits {
  implicit def arrayMultibandTileFastMapHistogramMonoid: Monoid[Array[FastMapHistogram]] = new Monoid[Array[FastMapHistogram]] {
    override def empty: Array[FastMapHistogram] = Array[FastMapHistogram]()

    override def combine(x: Array[FastMapHistogram],
                         y: Array[FastMapHistogram]): Array[FastMapHistogram] = {
      x.zipAll(y, Monoid[FastMapHistogram].empty, Monoid[FastMapHistogram].empty).map {
        case (x, y) => x.combine(y)
      }
    }
  }

  implicit def arrayMultibandTileStreamingHistogramMonoid: Monoid[Array[StreamingHistogram]] = new Monoid[Array[StreamingHistogram]] {
    override def empty: Array[StreamingHistogram] = Array[StreamingHistogram]()

    override def combine(x: Array[StreamingHistogram],
                         y: Array[StreamingHistogram]): Array[StreamingHistogram] = {
      x.zipAll(y, Monoid[StreamingHistogram].empty, Monoid[StreamingHistogram].empty).map {
        case (x, y) => x.combine(y)
      }
    }
  }
}
object Implicits extends Implicits
