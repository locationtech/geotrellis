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

import scala.collection.mutable
import scala.math.{min,max}
import geotrellis._
import Movement._

sealed trait Movement { val isVertical:Boolean }

/**
 * Movements used to move a [[Cursor]] around, and to track it's movements.
 */
object Movement { 
  val Up = new Movement { val isVertical = true }
  val Down = new Movement { val isVertical = true }
  val Left = new Movement { val isVertical = false }
  val Right = new Movement { val isVertical = false }
  val NoMovement = new Movement { val isVertical = false }
}

/** A lighweight wrapper around performing foreach calculations on a set of cell coordinates */
trait CellSet {
  /** Calls a funtion with the col and row coordinate of every cell contained in the CellSet.
   *
   * @param    f      Function that takes col and row coordinates, that will be called
   *                  for each cell contained in this CellSet.
   */
  def foreach(f:(Int,Int)=>Unit):Unit
}


object Cursor {
  /** Creates a cursor from a raster and a neighborhood
   *
   * This will create a [[Cursor]] based on the raster and neighborhood extent,
   * and will apply the neighborhood mask if it has one.
   *
   * @param    r          Raster the [[Cursor]] will be for.
   * @param    n          Neighborhood this [[Cursor]] will be based on,
   *                      for extent and mask.
   * @param analysisArea  Analysis area
   */
  def apply(r:Raster,n:Neighborhood,analysisArea:GridBounds):Cursor = {
    val result = new Cursor(r,analysisArea,n.extent)
    if(n.hasMask) { result.setMask(n.mask) }
    result
  }

  def apply(r:Raster,n:Neighborhood):Cursor =  apply(r,n,GridBounds(r))

  def apply(r:Raster,extent:Int):Cursor = new Cursor(r,GridBounds(r),extent)
}

/**
 * Represents a cursor that can be used to iterate over cells within a focal
 * neighborhood.
 *
 * @param      r                     Raster that this cursor runs over
 * @param      analysisArea          Analysis area
 * @param      ext                   The distance from the focus that the
 *                                   bounding box of this cursor extends.
 *                                   e.g. if the bounding box is 3x3, then
 *                                   the distance from center is 1.
 */
class Cursor(r:Raster, analysisArea:GridBounds, val extent:Int) {
  private val rows = r.rasterExtent.rows
  private val cols = r.rasterExtent.cols

  // How many columns from the left/top of the input raster does the analysis area begin?
  val analysisOffsetCols = analysisArea.colMin
  val analysisOffsetRows = analysisArea.rowMin

  private val d = 2*extent + 1

  private var mask:CursorMask = null
  private var hasMask = false

  // Values to track the bound of the cursor
  private var _colmin = 0
  private var _colmax = 0
  private var _rowmin = 0
  private var _rowmax = 0

  // Values to track added\removed values
  private var addedCol = 0
  private var removedCol = 0

  private var addedRow = 0
  private var removedRow = 0

   var movement = NoMovement

  // Values to track the focus of the cursor
  private var _col = 0
  private var _row = 0

  /** Indicates whether or not this cursor has been moved and is tracking state between
   *  the previous position and the current position */
  def isReset = movement == NoMovement

  /** 
   *  Cursor column relative to the analysis area.
   *
   *  For example, if the analysis area starts at col 2 and the focusX is currently 3,
   *  then the col should be 1. 
   */
  def col = _col - analysisOffsetCols

  /** Cursor row relative to the analysis area */
  def row = _row - analysisOffsetRows

  /**
   * Centers the cursor on a cell of the raster.
   * Added\Removed cells are not kept track of between centering moves,
   * and centering the cursor resets the state.
   *
   * @param   col    Column of raster to center on.
   * @param   row    Row of raster to center on.
   */
  def centerOn(col:Int,row:Int) = { 
    movement = NoMovement
    _col = col
    _row = row

    _colmin = max(0,_col - extent)
    _colmax = min(cols - 1, _col + extent)
    _rowmin = max(0, _row - extent)
    _rowmax = min(rows - 1, _row + extent)
  }

  /*
   * Move the cursor one cell space in a horizontal
   * of vertical direction. The cursor will keep track
   * of what cells became added by this move (covered by the cursor
   * or unmasked), and what cells became removed by this move
   * (no longer covered by the cursor or masked when previously unmasked).
   * The cursor will only keep the state of one move, so if two moves
   * are done in a row, the state of the first move is forgotten. Only
   * the difference between the cursor and it's most recent previous position
   * are accounted for.
   *
   * param     m     Movement enum that represents moving the cursor
   *                 Up,Down,Left or Right.
   */
  def move(m:Movement) = {
    movement = m
    m match {
      case Up => 
        addedRow = _rowmin - 1
        removedRow = _row + extent
        _row -= 1
      case Down =>
        addedRow = _rowmax + 1
        removedRow = _row - extent
        _row += 1
      case Left =>
        addedCol = _colmin - 1
        removedCol = _col + extent
        _col -= 1
      case Right =>
        addedCol = _colmax + 1
        removedCol = _col - extent
        _col += 1
      case _ => 
    }

    _colmin = max(0,_col - extent)
    _colmax = min(cols - 1, _col + extent)
    _rowmin = max(0, _row - extent)
    _rowmax = min(rows - 1, _row + extent)
  }

  /** Sets the mask for this cursor.
   *
   * @param     f    Function that takes a col and row of the neighborhood coordinates
   *                 and returns true if that cell should be masked.
   *                 The neighborhood coordinates are the size of the cursor's
   *                 bounding box, with (0,0) being the top right corner.
   */
  def setMask(f:(Int,Int) => Boolean) = {
    hasMask = true
    mask = new CursorMask(d,f)
  }

  /** A [[CellSet]] reperesenting all unmasked cells that are within the cursor bounds. */
  val allCells = new CellSet {
    def foreach(f:(Int,Int)=>Unit) = Cursor.this.foreach(f)
  }

  /** A [[CellSet]] reperesenting unmasked cells currently within the cursor bounds,
   *  that were added by the previous cursor movement. If the cursor has not been moved
   *  (i.e. if isReset == true) then addedCells represents the same thing
   *  as allCells.
   */
  val addedCells = new CellSet {
    def foreach(f:(Int,Int)=>Unit) = { Cursor.this.foreachAdded(f) }
  }

  /** A [[CellSet]] reperesenting cells that were moved outside the cursor bounds,
   *  or unmasked cells that were masked, by the previous cursor movement.
   *  If the cursor has not been moved this will be a no-op.
   */
  val removedCells = new CellSet {
    def foreach(f:(Int,Int)=>Unit) = Cursor.this.foreachRemoved(f)
  }

  /**
   * Iterates over all cell values of the raster which
   * are covered by the cursor and not masked.
   *
   * @param     f         Function that receives from each cell
   *                      it's col and row coordinates and it's value.
   */
  protected def foreach(f: (Int,Int)=>Unit):Unit = {
    if(!hasMask) {
      var y = _rowmin
      var x = 0
      while(y <= _rowmax) {
        x = _colmin
        while(x <= _colmax) {
          f(x,y)
          x += 1
        }
        y += 1
      }
    } else {
      var y = 0
      while(y < d) {
        mask.foreachX(y) { x =>
          val xRaster = x + (_col-extent)
          val yRaster = y + (_row-extent)
          if(_colmin <= xRaster && xRaster <= _colmax && _rowmin <= yRaster && yRaster <= _rowmax) {
            f(xRaster,yRaster)
          }
        }
        y += 1
      }
    }
  }

  /**
   * Iterates over all cell values of the raster which
   * are covered by the cursor and not masked, that were exposed
   * as part of the last move of the cursor.
   *
   * For instance, if move(Movement.Up) is called, then there will
   * potentially be a new row that is now covered by the cursor,
   * which are now covered. These values will be included for the
   * iterations of this function, as well any previously masked
   * cell values that were unmasked as part of the move.
   *
   * @param     f         Function that receives from each cell it's
   *                      col and row coordinates and it's value.
   */
  protected def foreachAdded(f:(Int,Int)=>Unit):Unit = {
    if(movement == NoMovement) {
      foreach(f) 
    } else if (movement.isVertical) {
      if(0 <= addedRow && addedRow < rows) {
        if(!hasMask) {
          var x = _colmin
          while(x <= _colmax) {
            f(x,addedRow)
            x += 1
          }
        } else {
          mask.foreachX(addedRow-(_row-extent)) { x =>
            val xRaster = x+(_col-extent)
            if(0 <= xRaster && xRaster <= cols) {
              f(xRaster,addedRow)
            }
          }
        }
      }        
    } else { // Horizontal
      if(0 <= addedCol && addedCol < cols) {
        if(!hasMask) {
          var y = _rowmin
          while(y <= _rowmax) {
            f(addedCol,y)
            y += 1
          }
        } else {
          if(movement == Left) {
            mask.foreachWestColumn { y =>
              val yRaster = y+(_row-extent)
              if(0 <= yRaster && yRaster < rows) {
                f(addedCol,yRaster)
              }
            }
          } else { // Right
            mask.foreachEastColumn { y =>
              val yRaster = y+(_row-extent)
              if(0 <= yRaster && yRaster < rows) {
                f(addedCol,yRaster)
              }
            }
          }
        }
      }        
    }

    if(hasMask) {
      mask.foreachUnmasked(movement) { (x,y) =>
        val xRaster = x+(_col-extent)
        val yRaster = y+(_row-extent)
        if(0 <= xRaster && xRaster < cols && 0 <= yRaster && yRaster < rows) {
          f(xRaster,yRaster)
        }
      }
    }
  }

  /**
   * Iterates over all cell values of the raster which
   * are no longer covered by the cursor 
   * as part of the last move last move of the cursor.
   *
   * For instance, if move(Movement.Up) is called, then there will
   * potentially be a new row at the bottom of the cursor that is now
   * uncovered by the cursor. These values will be included for the
   * iterations of this function, as well any previously unmasked
   * cell values that were masked as part of the move.
   *
   * @param     f         Function that receives from each cell it's
   *                      col and row coordinates and it's value.
   */
  protected def foreachRemoved(f:(Int,Int)=>Unit):Unit = {
    if(movement == NoMovement) { return }

    if(movement.isVertical) {
      if(0 <= removedRow && removedRow < rows) {
        if(!hasMask) {
          var x = _colmin
          while(x <= _colmax) {
            f(x,removedRow)
            x += 1
          }
        } else {
          if(movement == Up) {
            mask.foreachX(d-1) { x =>
              val xRaster = x+(_col-extent)
              if(0 <= xRaster && xRaster < cols) {
                f(xRaster,removedRow)
              }
            }
          }
          else { // Down
            mask.foreachX(0) { x =>
              val xRaster = x+(_col-extent)
              if(0 <= xRaster && xRaster < cols) {
                f(xRaster,removedRow)
              }
            }
          }
        }
      }
    } else { // Horizontal
      if(0 <= removedCol && removedCol < cols) {
        if(!hasMask) {
          var y = _rowmin
          while(y <= _rowmax) {
            f(removedCol,y)
            y += 1
          }
        } else {
          if(movement == Left) {
            mask.foreachEastColumn { y =>
              val yRaster = y+(_row-extent)
              if(0 <= yRaster && yRaster < rows) {
                f(removedCol,yRaster)
              }
            }
          } else { //Right
            mask.foreachWestColumn { y =>
              val yRaster = y+(_row-extent)
              if(0 <= yRaster && yRaster < rows) {
                f(removedCol,yRaster)
              }
            }
          }
        }
      }
    }

    if(hasMask) {
      mask.foreachMasked(movement) { (x,y) =>
        val xRaster = x+(_col-extent)
        val yRaster = y+(_row-extent)
        if(0 <= xRaster && xRaster < cols && 0 <= yRaster && yRaster < rows) {
          f(xRaster,yRaster)
        }
      }
    }
  }

  def asciiDraw:String = {
    val sb = new StringBuilder
    var row = 0
    allCells.foreach { (cl,rw) =>
      if(row != rw) { sb.append("\n") ; row += 1 }
      val s = r.get(cl,rw).toString
      val pad = " " * math.max(6 - s.length,0)
      sb.append(s"$pad$s")
    }
    sb.toString
  }
}
