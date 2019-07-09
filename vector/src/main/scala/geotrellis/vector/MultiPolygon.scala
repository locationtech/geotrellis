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

package geotrellis.vector

import geotrellis.vector.GeomFactory._

import org.locationtech.jts.{geom => jts}
import spire.syntax.cfor._

trait MultiPolygonConstructors {
  lazy val EMPTY = apply(Seq[jts.Polygon]())

  def apply(ps: jts.Polygon*): jts.MultiPolygon =
    apply(ps)

  def apply(ps: Traversable[jts.Polygon]): jts.MultiPolygon =
    factory.createMultiPolygon(ps.toArray)

  def apply(ps: Array[jts.Polygon]): jts.MultiPolygon = {
    val len = ps.length
    val arr = Array.ofDim[jts.Polygon](len)
    cfor(0)(_ < len, _ + 1) { i =>
      arr(i) = ps(i)
    }

    factory.createMultiPolygon(arr)
  }
}

object MultiPolygon extends MultiPolygonConstructors
