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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import scala.math._

/**
 * Computes the maximum value of a neighborhood for a given raster.
 *
 * @param    r      Tile on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Max(r: Op[Tile], n: Op[Neighborhood], tns: Op[TileNeighbors]) extends FocalOp(r, n, tns)({
  (r, n) =>
    if(r.cellType.isFloatingPoint) {
       new CursorCalculation[Tile] with DoubleArrayTileResult { 
         def calc(r: Tile, cursor: Cursor) = {
           var m = Double.MinValue
           cursor.allCells.foreach { (x, y) => 
             val v = r.getDouble(x, y)
             if(v > m) { m = v }
          }
          tile.setDouble(cursor.col, cursor.row, m)
        }  
      }
      
    }else{
      new CursorCalculation[Tile] with IntArrayTileResult { 
        def calc(r: Tile, cursor: Cursor) = {
          var m = Int.MinValue
          cursor.allCells.foreach { (x, y) => 
            val v = r.get(x, y)
            if(v > m) { m = v }
          }
          tile.set(cursor.col, cursor.row, m)
        }  
      }
    } 
})

object Max {
  def apply(r: Op[Tile], n: Op[Neighborhood]) = new Max(r, n, TileNeighbors.NONE)
}
