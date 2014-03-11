/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.TileNeighbors

import scala.math._

/** Computes the minimum value of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 *
 * @note            Min does not currently support Double raster data.
 *                  If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                  the data values will be rounded to integers.
 */
case class Min(r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors]) extends FocalOp[Raster](r,n,tns)({
  (r,n) =>
    if(r.isFloat){
      new CursorCalculation[Raster] with DoubleRasterDataResult {
        def calc(r:Raster,cursor:Cursor) = {
  
          var m:Double = Double.NaN
          cursor.allCells.foreach { 
            (col,row) => {
              val v = r.getDouble(col,row)
              if(isData(v) && (v < m || isNoData(m))) { m = v }
            }
          }
          data.setDouble(cursor.col,cursor.row,m)
        }
      }
 
    }else{
      new CursorCalculation[Raster] with IntRasterDataResult {
        def calc(r:Raster,cursor:Cursor) = {
  
          var m = NODATA
          cursor.allCells.foreach { 
            (col,row) => {
              val v = r.get(col,row)
              if(isData(v) && (v < m || isNoData(m))) { m = v }
            }
          }
          data.set(cursor.col,cursor.row,m)
        }
      }
    }
})

object Min {
  def apply(r:Op[Raster], n:Op[Neighborhood]) = new Min(r,n,TileNeighbors.NONE)
}
