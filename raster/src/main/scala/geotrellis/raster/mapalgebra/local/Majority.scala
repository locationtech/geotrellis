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

package geotrellis.raster.mapalgebra.local

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import spire.syntax.cfor._
import scala.collection.mutable


object Majority extends Serializable {
  def apply(r: Tile*): Tile = apply(0, r)

  def apply(rs: Traversable[Tile])(implicit d: DI): Tile = apply(0, rs)

  def apply(level: Int, r: Tile*): Tile = apply(level, r)

  def apply(level: Int, rs: Traversable[Tile])(implicit d: DI): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.toSeq.length
    if(layerCount == 0) {
      sys.error("Can't compute majority of empty sequence")
    } else {
      val newCellType = rs.map(_.cellType).reduce(_.union(_))
      val Dimensions(cols, rows) = rs.head.dimensions
      val tile = ArrayTile.alloc(newCellType, cols, rows)

      if(newCellType.isFloatingPoint) {
        val counts = mutable.Map[Double, Int]()

        cfor(0)(_ < rows, _ + 1) { row =>
          cfor(0)(_ < cols, _ + 1) { col =>
            counts.clear
            for(r <- rs) {
              val v = r.getDouble(col, row)
              if(isData(v)) {
                if(!counts.contains(v)) {
                  counts(v) = 1
                } else {
                  counts(v) += 1
                }
              }
            }

            val sorted =
              counts.keys
                .toSeq
                .sortBy { k => counts(k) }
                .toList
            val len = sorted.length - 1
            val m =
              if(len >= level) { sorted(len-level) }
              else { Double.NaN }
            tile.setDouble(col, row, m)
          }
        }
      } else {
        val counts = mutable.Map[Int, Int]()

        cfor(0)(_ < rows, _ + 1) { row =>
          cfor(0)(_ < cols, _ + 1) { col =>
            counts.clear
            for(r <- rs) {
              val v = r.get(col, row)
              if(isData(v)) {
                if(!counts.contains(v)) {
                  counts(v) = 1
                } else {
                  counts(v) += 1
                }
              }
            }

            val sorted =
              counts.keys
                .toSeq
                .sortBy { k => counts(k) }
                .toList
            val len = sorted.length - 1
            val m =
              if(len >= level) { sorted(len-level) }
              else { NODATA }
            tile.set(col, row, m)
          }
        }
      }
      tile
    }
  }
}

trait MajorityMethods extends MethodExtensions[Tile] {
  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rs: Traversable[Tile]): Tile =
    Majority(Traversable(self) ++ rs)

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rs: Tile*)(implicit d: DI): Tile =
    localMajority(rs)

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rs: Traversable[Tile]): Tile =
    Majority(n, Traversable(self) ++ rs)

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rs: Tile*)(implicit d: DI): Tile =
    localMajority(n, rs)
}
