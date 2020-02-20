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

trait MultiPointConstructors {
  lazy val EMPTY = apply(Seq[jts.Point]())

  def apply(ps: jts.Point*): jts.MultiPoint =
    apply(ps)

  def apply(ps: Traversable[jts.Point]): jts.MultiPoint =
    factory.createMultiPoint(ps.toArray)

  def apply(ps: Array[jts.Point]): jts.MultiPoint = {
    val len = ps.length
    val arr = Array.ofDim[jts.Point](len)
    cfor(0)(_ < len, _ + 1) { i =>
      arr(i) = ps(i)
    }

    factory.createMultiPoint(arr)
  }

  def apply(ps: Traversable[(Double, Double)])(implicit d: DummyImplicit): jts.MultiPoint =
    factory.createMultiPointFromCoords(ps.map { p => new jts.Coordinate(p._1, p._2) }.toArray)
}

object MultiPoint extends MultiPointConstructors
