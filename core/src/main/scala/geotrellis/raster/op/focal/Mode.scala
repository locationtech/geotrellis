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
import geotrellis.statistics.FastMapHistogram

/** Computes the mode of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 * @note            Mode does not currently support Double raster data.
 *                  If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                  the data values will be rounded to integers.
 */
case class Mode(r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors]) extends FocalOp[Raster](r,n,tns)({
  (r,n) => 
    n match {
      case Square(ext) => new CellwiseModeCalc(ext)
      case _ => new CursorModeCalc(n.extent)
    }
})

object Mode {
  def apply(r:Op[Raster],n:Op[Neighborhood]) = new Mode(r,n,TileNeighbors.NONE)
}

class CursorModeCalc(extent:Int) extends CursorCalculation[Raster] with IntArrayTileResult 
                                                                   with MedianModeCalculation {
  initArray(extent)
                                                         
  def calc(r:Raster,cursor:Cursor) = {
    cursor.removedCells.foreach { (x,y) =>
      val v = r.get(x,y)
      if(isData(v)) {
        removeValue(v)
      }
    }
    cursor.addedCells.foreach { (x,y) =>
      val v = r.get(x,y)
      if(isData(v)) addValue(v)
    }
    data.set(cursor.col,cursor.row,mode)
  }
}

class CellwiseModeCalc(extent:Int) extends CellwiseCalculation[Raster] with IntArrayTileResult 
                                                                       with MedianModeCalculation {
  initArray(extent)

  def add(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (isData(v)) {
      addValue(v)
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (isData(v)) {
      removeValue(v)
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y,mode) }
}
