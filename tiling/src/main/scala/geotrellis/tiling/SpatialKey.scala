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

package geotrellis.tiling

import geotrellis.vector.Extent

/** A SpatialKey designates the spatial positioning of a layer's tile. */
case class SpatialKey(col: Int, row: Int) extends Product2[Int, Int] {
  def _1 = col
  def _2 = row

  /** Retrieve the [[Extent]] that corresponds to this key, given a layout. */
  def extent(layout: LayoutDefinition): Extent = layout.mapTransform.keyToExtent(this)
}

object SpatialKey {
  implicit def tupToKey(tup: (Int, Int)): SpatialKey =
    SpatialKey(tup._1, tup._2)

  implicit def keyToTup(key: SpatialKey): (Int, Int) =
    (key.col, key.row)

  implicit def ordering[A <: SpatialKey]: Ordering[A] =
    Ordering.by(sk => (sk.col, sk.row))

  implicit object Boundable extends Boundable[SpatialKey] {
    def minBound(a: SpatialKey, b: SpatialKey) = {
      SpatialKey(math.min(a.col, b.col), math.min(a.row, b.row))
    }
    def maxBound(a: SpatialKey, b: SpatialKey) = {
      SpatialKey(math.max(a.col, b.col), math.max(a.row, b.row))
    }
  }
}
