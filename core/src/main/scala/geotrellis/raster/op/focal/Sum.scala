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

/** Computes the sum of values of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 *
 * @note            If the neighborhood is a [[Square]] neighborhood, the sum calucation will use
 *                  the [[CellwiseSumCalc]] to perform the calculation, because it is faster.
 *                  If the neighborhood is of any other type, then [[CursorSumCalc]] is used.
 *
 * @note            Sum does not currently support Double raster data.
 *                  If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                  the data values will be rounded to integers.
 */
case class Sum(r:Op[Raster], n:Op[Neighborhood],ns:Op[TileNeighbors]) extends FocalOp[Raster](r,n,ns)({ 
  (r,n) =>
    if(r.isFloat){
      n match {
        case Square(ext) => new CellwiseDoubleSumCalc
        case _ => new CursorDoubleSumCalc
      }
    }else{
      n match {
        case Square(ext) => new CellwiseSumCalc
        case _ => new CursorSumCalc
      }
    }
})

object Sum {
  def apply(r:Op[Raster], n:Op[Neighborhood]) = new Sum(r,n,TileNeighbors.NONE)
}

class CursorSumCalc extends CursorCalculation[Raster] 
    with IntRasterDataResult {
  var total = 0

  def calc(r:Raster,cursor:Cursor) = {

    val added = collection.mutable.Set[(Int,Int,Int)]()
    cursor.addedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      added += ((x,y,v))
      if(isData(v)) { total += r.get(x,y) }
    }

    val removed = collection.mutable.Set[(Int,Int,Int)]()
    cursor.removedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      removed += ((x,y,v))
      if(isData(v)) { total -= r.get(x,y) }
    }

    data.set(cursor.col,cursor.row,total)
  }
}

class CellwiseSumCalc extends CellwiseCalculation[Raster]
    with IntRasterDataResult {
  var total = 0
  
  def add(r:Raster,x:Int,y:Int) = { 
    val v = r.get(x,y)
    if(isData(v)) { total += r.get(x,y) }
  }

  def remove(r:Raster,x:Int,y:Int) = { 
    val v = r.get(x,y)
    if(isData(v)) { total -= r.get(x,y) }
  }

  def reset() = { total = 0}
  def setValue(x:Int,y:Int) = { 
    data.set(x,y,total) 
  }
}

class CursorDoubleSumCalc extends CursorCalculation[Raster] 
    with DoubleRasterDataResult {
  var total = 0.0

  def calc(r:Raster,cursor:Cursor) = {

    val added = collection.mutable.Set[(Int,Int,Double)]()
    cursor.addedCells.foreach { (x,y) => 
      val v = r.getDouble(x,y)
      added += ((x,y,v))
      if(isData(v)) { total += r.getDouble(x,y) }
    }

    val removed = collection.mutable.Set[(Int,Int,Double)]()
    cursor.removedCells.foreach { (x,y) => 
      val v = r.getDouble(x,y)
      removed += ((x,y,v))
      if(isData(v)) { total -= r.getDouble(x,y) }
    }

    data.setDouble(cursor.col,cursor.row,total)
  }
}

class CellwiseDoubleSumCalc extends CellwiseCalculation[Raster]
    with DoubleRasterDataResult {
  var total = 0.0
  
  def add(r:Raster,x:Int,y:Int) = { 
    val v = r.getDouble(x,y)
    if(isData(v)) { total += r.getDouble(x,y) }
  }

  def remove(r:Raster,x:Int,y:Int) = { 
    val v = r.getDouble(x,y)
    if(isData(v)) { total -= r.getDouble(x,y) }
  }

  def reset() = { total = 0.0 }
  def setValue(x:Int,y:Int) = { 
    data.setDouble(x,y,total) 
  }
}
