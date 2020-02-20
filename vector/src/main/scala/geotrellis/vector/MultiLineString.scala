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

/** Companion object to [[MultiLine]] */
trait MultiLineStringConstructors {
  lazy val EMPTY = apply(Seq[jts.LineString]())

  def apply(ls: jts.LineString*): jts.MultiLineString =
    apply(ls)

  def apply(ls: Traversable[jts.LineString]): jts.MultiLineString =
    factory.createMultiLineString(ls.toArray)

  def apply(ls: Array[jts.LineString]): jts.MultiLineString = {
    val len = ls.length
    val arr = Array.ofDim[jts.LineString](len)
    cfor(0)(_ < len, _ + 1) { i =>
      arr(i) = ls(i)
    }

    factory.createMultiLineString(arr)
  }
}

object MultiLineString extends MultiLineStringConstructors
