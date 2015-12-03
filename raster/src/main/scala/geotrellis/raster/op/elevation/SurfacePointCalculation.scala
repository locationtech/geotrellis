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

package geotrellis.raster.op.elevation

import geotrellis.raster._
import geotrellis.raster.op.focal.{FocalCalculation, Neighborhood}

import scala.math._

/** A representation of the approximated partial derivatives of a raster cell,
 *  and the slope and aspect that can be calculated from those values.
 *  Trigonometric values of aspect and slope, computed using the partial derivatives
 *  instead of library trigonometric functions, are also exposed for performance.
 */
class SurfacePoint() {
  /** True if the partial derivatives at this point don't exist */
  var isNaN = false

  /** Partial derivative of z with respect to x */
  var `dz/dx` = Double.NaN

  /** Partial derivative of z with respect to y */
  var `dz/dy` = Double.NaN

  def aspect() = {
    var a = atan2(`dz/dy`, -`dz/dx`)

    if (`dz/dx` == 0 && `dz/dy` == 0) {
      /* Flat area */
      a = Double.NaN
    } else {
      if (a < 0) { a += 2*Pi }
    }

    if (a == 2*Pi) { a = 0.0 }
    a
  }
  def slope(zFactor: Double): Double = atan(zFactor * sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy`))
  def slope(): Double = slope(1.0)

  // Trig functions for slope and aspect.
  // Use these if you want to get the sine or cosine of the aspect or slope,
  // since they are a lot more performant than calling scala.math.sin or 
  // scala.math.cos

  /** Cosine of the slope, computed using partial derivatives */
  def cosSlope = {
    val denom = sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy` + 1)
    if(denom == 0) Double.NaN else {
      1/denom
    }
  }

  /** Sine of the slope, computed using partial derivatives */
  def sinSlope = {
    val denom = sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy` + 1)
    if(denom == 0) Double.NaN else {
      sqrt(`dz/dx` * `dz/dx` + `dz/dy` * `dz/dy`) / denom
    }
  }

  /** Cosine of the aspect, computed using partial derivatives */
  def cosAspect = {
    if(`dz/dx` == 0) { if(`dz/dy` == 0) -1 else 0 } else {
      if(`dz/dy` == 0) {
        if(`dz/dx` < 0) 1 else -1
      } else {
        -`dz/dx` / sqrt(`dz/dy`*`dz/dy` + `dz/dx`*`dz/dx`)
      }
    }
  }

  /** Sine of the aspect, computed using partial derivatives */
  def sinAspect = {
    if(`dz/dy` == 0) 0 else {
      if(`dz/dx` == 0) {
        if(`dz/dy` < 0) -1 else if(`dz/dy` > 0) 1 else 0
      } else {
        `dz/dy` / sqrt(`dz/dy`*`dz/dy` + `dz/dx`*`dz/dx`)
      }
    }
  }
}

/** Calculation used for surface point calculations such as Slope, Aspect, and Hillshade
 *
 * Uses a specific traversal strategy for performance benefits.
 *
 * @note
 * For edge cells, the neighborhood points that lie outside the extent of the raster
 * will be counted as having the same value as the focal point.
 */
abstract class SurfacePointCalculation[T](r: Tile, n: Neighborhood, analysisArea: Option[GridBounds], val cellSize: CellSize)
  extends FocalCalculation[T](r, n, analysisArea)
{
  var lastY = -1

  var cellWidth = 0.0
  var cellHeight = 0.0

  var west = new Array[Double](3)
  var base = new Array[Double](3)
  var east = new Array[Double](3)

  val s = new SurfacePoint

  /** Sets a result at (x, y) from a [[SurfacePoint]]
   *
   * Implementors need to provide this function to store the
   * results of the surface point calculation.
   */
  def setValue(x: Int, y: Int, s: SurfacePoint): Unit

  def setValue(x: Int, y: Int): Unit = {
    calcSurface()
    setValue(x, y, s)
  }
  
  def moveRight() = {
    val tmp = west
    west = base
    base = east
    east = tmp
  }
  
  protected def calcSurface(): Unit = {
    if(isNoData(base(1))) {
      s.`dz/dx` = Double.NaN
      s.`dz/dy` = Double.NaN
    } else {
      val neValue = if(isData(east(0))) east(0) else base(1)
      val eValue = if(isData(east(1))) east(1) else base(1)
      val seValue = if(isData(east(2))) east(2) else base(1)
      val nValue = if(isData(base(0))) base(0) else base(1)
      val sValue = if(isData(base(2))) base(2) else base(1)
      val nwValue = if(isData(west(0))) west(0) else base(1)
      val wValue = if(isData(west(1))) west(1) else base(1)
      val swValue = if(isData(west(2))) west(2) else base(1)

      s.`dz/dx` = (neValue + 2*eValue + seValue - nwValue - 2*wValue - swValue) / (8 * cellWidth)
      s.`dz/dy` = (swValue + 2*sValue + seValue - nwValue - 2*nValue - neValue) / (8 * cellHeight)
    }
  }

  /**
   * Executes a specific traversal strategy for SurfacePointCalculation.
   * The difference between this and ScanLine for CellwiseCalculation is that for edge cases,
   * the value at the focus is added in place of out-of-border neighborhood
   * values.
   *
   * @note                    Assumes a [[Square]](1) neighborhood.
   * @note                    All values in the neighborhood that are outside the raster grid
   *                          are counted as having the focal value. Note that in the case
   *                          the cell is outside the analysis area, but still inside the raster,
   *                          the raster value will still be used.
   */
  def execute(): T = {
    val colMin = bounds.colMin
    val colMax = bounds.colMax

    val rowMin = bounds.rowMin
    val rowMax = bounds.rowMax

    val colBorderMax = r.cols - 1
    val rowBorderMax = r.rows - 1

    cellWidth = cellSize.width
    cellHeight = cellSize.height

    if(colBorderMax < 3 || rowBorderMax < 3) { 
      sys.error(s"Tile is too small to get surface values. ($colBorderMax, $rowBorderMax)") 
    }

    def getValSafe(col: Int, row: Int, focalVal: Double) = {
      if(col < 0 || colBorderMax < col || row < 0 || rowBorderMax < row) {
        focalVal
      } else {
        r.getDouble(col, row)
      }
    }

    var focalValue = r.getDouble(colMin, rowMin)
    
    // Handle top row

    /// Top Left
    west(0) = getValSafe(colMin-1, rowMin-1, focalValue)
    west(1) = getValSafe(colMin-1, rowMin  , focalValue)
    west(2) = getValSafe(colMin-1, rowMin+1, focalValue)
    base(0) = getValSafe(colMin  , rowMin-1, focalValue)
    base(1) = focalValue
    base(2) = r.getDouble(colMin, rowMin + 1)
    east(0) = getValSafe(colMin+1, rowMin-1, focalValue)
    east(1) = r.getDouble(colMin + 1, rowMin)
    east(2) = r.getDouble(colMin + 1, rowMin + 1)
    setValue(0, 0)
    
    var col = colMin + 1

    /// Top Middle
    while (col < colMax) {
      moveRight()
      focalValue = r.getDouble(col, rowMin)
      west(0) = getValSafe(col-1, rowMin-1, focalValue)
      base(0) = getValSafe(col  , rowMin-1, focalValue)
      east(0) = getValSafe(col+1, rowMin-1, focalValue)
      east(1) = r.getDouble(col+1, rowMin)
      east(2) = r.getDouble(col+1, rowMin + 1)
      setValue(col-colMin, 0)
      col += 1
    }

    /// Top Right
    moveRight()
    focalValue = r.getDouble(col, rowMin)
    west(0) = getValSafe(col-1, rowMin-1, focalValue)
    base(0) = getValSafe(col  , rowMin-1, focalValue)
    east(0) = getValSafe(col+1, rowMin-1, focalValue)
    east(1) = getValSafe(col+1, rowMin  , focalValue)
    east(2) = getValSafe(col+1, rowMin+1, focalValue)
    setValue(col-colMin, 0)
    
    var row = rowMin + 1

    // Handle middle rows
    while (row < rowMax) {
      focalValue = r.getDouble(colMin, row)
      // Middle Left
      west(0) = getValSafe(colMin-1, row-1, focalValue)
      west(1) = getValSafe(colMin-1, row, focalValue)
      west(2) = getValSafe(colMin-1, row+1, focalValue)
      base(0) = r.getDouble(colMin, row-1)
      base(1) = focalValue
      base(2) = r.getDouble(colMin, row+1)
      east(0) = r.getDouble(colMin+1, row-1)
      east(1) = r.getDouble(colMin+1, row)
      east(2) = r.getDouble(colMin+1, row+1)
      setValue(0, row-rowMin)

      /// Middle Middle
      col = colMin + 1
      while (col < colMax) {
        moveRight()
        east(0) = r.getDouble(col+1, row-1)
        east(1) = r.getDouble(col+1, row)
        east(2) = r.getDouble(col+1, row+1)
        setValue(col-colMin, row-rowMin)
        col += 1
      }

      /// Middle Right
      moveRight()
      focalValue = r.getDouble(col, row)

      east(0) = getValSafe(col+1, row-1, focalValue)
      east(1) = getValSafe(col+1, row  , focalValue)
      east(2) = getValSafe(col+1, row+1, focalValue)

      setValue(col-colMin, row-rowMin)

      row += 1
    }

    // Handle bottom row

    /// Bottom Left
    focalValue = r.getDouble(colMin, row)
    west(0) = getValSafe(colMin-1, row-1, focalValue)
    west(1) = getValSafe(colMin-1, row  , focalValue)
    west(2) = getValSafe(colMin-1, row+1, focalValue)
    base(0) = r.getDouble(colMin, row-1)
    base(1) = focalValue
    base(2) = getValSafe(colMin  , row+1, focalValue)
    east(0) = r.getDouble(colMin+1, row-1)
    east(1) = r.getDouble(colMin+1, row)
    east(2) = getValSafe(colMin+1, row+1, focalValue)
    setValue(0, row-rowMin)

    /// Bottom Middle
    col = colMin + 1
    while (col < colMax) {
      moveRight()
      focalValue = r.getDouble(col, row)
      west(2) = getValSafe(col-1, row+1, focalValue)
      base(2) = getValSafe(col  , row+1, focalValue)
      east(0) = r.getDouble(col+1, row-1)
      east(1) = r.getDouble(col+1, row)
      east(2) = getValSafe(col+1, row+1, focalValue)
      setValue(col-colMin, row-rowMin)
      col += 1
    }

    /// Bottom Right
    moveRight()
    focalValue = r.getDouble(col, row)
    west(2) = getValSafe(col-1, row+1, focalValue)
    base(2) = getValSafe(col  , row+1, focalValue)
    east(0) = getValSafe(col+1, row-1, focalValue)
    east(1) = getValSafe(col+1, row  , focalValue)
    east(2) = getValSafe(col+1, row+1, focalValue)
    setValue(col-colMin, row-rowMin)

    result
  }
}
