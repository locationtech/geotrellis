/*
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
 */

package geotrellis.raster.op.local

import geotrellis.raster._

import scalaxy.loops._

/**
 * The mean of values at each location in a set of Tiles.
 */
object Mean extends Serializable {
  def apply(rs: Traversable[Tile]): Tile = 
    apply(rs.toSeq)

  def apply(rs: Tile*)(implicit d: DI): Tile = 
    apply(rs)

  def apply(rs: Seq[Tile]): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.length
    if(layerCount == 0) {
      sys.error(s"Can't compute mean of empty sequence")
    } else {
      val newCellType = rs.map(_.cellType).reduce(_.union(_))
      val (cols, rows) = rs(0).dimensions
      val tile = ArrayTile.alloc(newCellType, cols, rows)
      if(newCellType.isFloatingPoint) {
        for(col <- 0 until cols optimized) {
          for(row <- 0 until rows optimized) {
            var count = 0
            var sum = 0.0
            for(i <- 0 until layerCount optimized) {
              val v = rs(i).getDouble(col, row)
              if(isData(v)) {
                count += 1
                sum += v
              }
            }

            if(count > 0) {
              tile.setDouble(col, row, sum/count)
            } else {
              tile.setDouble(col, row, Double.NaN)
            }
          }
        }
      } else {
        for(col <- 0 until cols optimized) {
          for(row <- 0 until rows optimized) {
            var count = 0
            var sum = 0
            for(i <- 0 until layerCount optimized) {
              val v = rs(i).get(col, row)
              if(isData(v)) {
                count += 1
                sum += v
              }
            }
            if(count > 0) {
              tile.set(col, row, sum/count)
            } else {
              tile.set(col, row, NODATA)
            }
          }
        }
      }
      tile
    }
  }
}
