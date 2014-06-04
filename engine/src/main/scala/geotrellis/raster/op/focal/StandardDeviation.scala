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
 * Computes the standard deviation of a neighborhood for a given raster. Returns a raster of TypeDouble.
 *
 * @param    r      Tile on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns     TileNeighbors that describe the neighboring tiles.
 *
 * @note            StandardDeviation does not currently support Double raster data inputs.
 *                  If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
 *                  the data values will be rounded to integers.
 */
case class StandardDeviation(r:Op[Tile],n:Op[Neighborhood],tns:Op[TileNeighbors]) extends FocalOp[Tile](r,n,tns)({
  (r,n) => 
    if(r.cellType.isFloatingPoint) {
      new CursorCalculation[Tile] with DoubleArrayTileResult {
        var count:Int = 0
        var sum:Double = 0

        def calc(r:Tile,c:Cursor) = {
          c.removedCells.foreach { (x,y) => 
            val v = r.getDouble(x,y)
            if(isData(v)) { count -= 1; sum -= v } 
          }
          
          c.addedCells.foreach { (x,y) => 
            val v = r.getDouble(x,y)
            if(isData(v)) { count += 1; sum += v }
          }

          val mean = sum / count.toDouble
          var squares = 0.0

          c.allCells.foreach { (x,y) =>
            var v = r.getDouble(x,y)
            if(isData(v)) { 
              squares += math.pow(v - mean,2)
            }
          }

          tile.setDouble(c.col,c.row,math.sqrt(squares / count.toDouble))
        }
      }
    } else {
      new CursorCalculation[Tile] with DoubleArrayTileResult {
      var count:Int = 0
      var sum:Int = 0

      def calc(r:Tile,c:Cursor) = {
        c.removedCells.foreach { (x,y) => 
          val v = r.get(x,y)
          if(isData(v)) { count -= 1; sum -= v } 
        }
        
        c.addedCells.foreach { (x,y) => 
          val v = r.get(x,y)
          if(isData(v)) { count += 1; sum += v }
        }

        val mean = sum / count.toDouble
        var squares = 0.0

        c.allCells.foreach { (x,y) =>
          var v = r.get(x,y)
          if(isData(v)) { 
            squares += math.pow(v - mean,2)
          }
        }
        tile.setDouble(c.col,c.row,math.sqrt(squares / count.toDouble))
      }
    }
  }
})


object StandardDeviation {
  def apply(r:Op[Tile], n:Op[Neighborhood]) = new StandardDeviation(r,n,TileNeighbors.NONE)
}
