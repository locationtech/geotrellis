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

/** Computes the mean value of a neighborhood for a given raster. Returns a raster of TypeDouble
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.

 * @return          Returns a double value raster that is the computed mean for each neighborhood.
 *
 * @note            If the neighborhood is a [[Square]] neighborhood, the mean calucation will use
 *                  the [[CellwiseMeanCalc]] to perform the calculation, because it is faster.
 *                  If the neighborhood is of any other type, then [[CursorMeanCalc]] is used.
 */
case class Mean(r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors]) 
    extends FocalOp[Raster](r,n,tns)({
  (r,n) =>
      if(r.isFloat) {
        n match {
          case Square(ext) => new CellwiseMeanCalcDouble
          case _ => new CursorMeanCalcDouble
        }
      } else {
        n match {
          case Square(ext) => new CellwiseMeanCalc
          case _ => new CursorMeanCalc
        }
      }
})

object Mean {
  def apply(r:Op[Raster],n:Op[Neighborhood]) = new Mean(r,n,TileNeighbors.NONE)
}

case class CursorMeanCalc() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var sum:Int = 0

  def calc(r:Raster,c:Cursor) = {
    c.removedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      if(isData(v)) { count -= 1; sum -= v } 
    }
    c.addedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      if(isData(v)) { count += 1; sum += v } 
    }
    data.setDouble(c.col,c.row,sum / count.toDouble)
  }
}

case class CellwiseMeanCalc() extends CellwiseCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var sum:Int = 0

 def add(r:Raster, x:Int, y:Int) = {
    val z = r.get(x,y)
    if (isData(z)) {
      count += 1
      sum   += z
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val z = r.get(x,y)
    if (isData(z)) {
      count -= 1
      sum -= z
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y, sum / count.toDouble) }
  def reset() = { count = 0 ; sum = 0 }
}

case class CursorMeanCalcDouble() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var sum:Double = 0.0

  def calc(r:Raster,c:Cursor) = {
    c.removedCells.foreach { (x,y) => 
      val v = r.getDouble(x,y)
      if(isData(v)) { count -= 1; sum -= v } 
    }
    c.addedCells.foreach { (x,y) => 
      val v = r.getDouble(x,y)
      if(isData(v)) { count += 1; sum += v } 
    }
    data.setDouble(c.col,c.row,sum / count)
  }
}

case class CellwiseMeanCalcDouble() extends CellwiseCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var sum:Double = 0.0

 def add(r:Raster, x:Int, y:Int) = {
    val z = r.getDouble(x,y)
    if (isData(z)) {
      count += 1
      sum   += z
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val z = r.getDouble(x,y)
    if (isData(z)) {
      count -= 1
      sum -= z
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y, sum / count) }
  def reset() = { count = 0 ; sum = 0.0 }
}
