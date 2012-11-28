package geotrellis.raster.op.focal

import scala.collection.mutable
import scala.math.{min,max}

import geotrellis._

sealed trait Movement { val isVertical:Boolean }

object Movement { 
  val Up = new Movement { val isVertical = true }
  val Down = new Movement { val isVertical = true }
  val Left = new Movement { val isVertical = false }
  val Right = new Movement { val isVertical = false }
  val NoMovement = new Movement { val isVertical = false }
}
import Movement._

object Cursor {
  def getInt(r:Raster,n:Neighborhood) = {
    val cur = new IntCursor(r,n.extent)
    if(n.hasMask) { cur.setMask(n.mask) }
    cur
  }

  def getDouble(r:Raster,n:Neighborhood) = {
    val cur = new DoubleCursor(r,n.extent)
    if(n.hasMask) { cur.setMask(n.mask) }
    cur
  }
}

trait CellSet[@specialized(Int,Double)D] {
  def foreach(f:D=>Unit):Unit
}

/**
 * Represents a cursor that can be used to iterate over cells within a focal
 * neighborhood.
 *
 * @param      r                     Raster that this cursor runs over
 * @param      distanceFromCenter    The distance from the focus that the
 *                                   bounding box of this cursor extends.
 *                                   e.g. if the bounding box is 9x9, then
 *                                   the distance from center is 1.
 */
abstract class Cursor[@specialized(Int,Double) D](r:Raster, distanceFromCenter:Int) {
  protected val raster = r

  val dim = distanceFromCenter
  private val d = 2*dim + 1

  var mask:CursorMask = null
  private var hasMask = false

  val addedCells = new CellSet[D] {
    def foreach(f:D=>Unit) = Cursor.this.foreachAdded(f)
  }

  val removedCells = new CellSet[D] {
    def foreach(f:D=>Unit) = Cursor.this.foreachRemoved(f)
  }

  // Values to track the bound of the cursor
  private var xmin = 0
  private var xmax = 0
  private var ymin = 0
  private var ymax = 0

  // Values to track added\removed values
  private var addedCol = 0
  private var removedCol = 0

  private var addedRow = 0
  private var removedRow = 0

  private var movement = NoMovement

  // Values to track the focus of the cursor
  private var _focusX = 0
  private var _focusY = 0

  protected def get(x:Int,y:Int):D

  def focusX = _focusX
  def focusY = _focusY

  /*
   * Centers the cursor on a cell of the raster.
   * Added\Removed cells are not kept track of between centering moves,
   * and centering the cursor resets the state.
   */
  def centerOn(x:Int,y:Int) = { 
    movement = NoMovement
    _focusX = x
    _focusY = y

    setBounds()
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
        addedRow = ymin - 1
        removedRow = _focusY + dim
        _focusY -= 1
      case Down =>
        addedRow = ymax + 1
        removedRow = _focusY - dim
        _focusY += 1
      case Left =>
        addedCol = xmin - 1
        removedCol = _focusX + dim
        _focusX -= 1
      case Right =>
        addedCol = xmax + 1
        removedCol = _focusX - dim
        _focusX += 1
      case _ => 
    }

    setBounds()
  }

  @inline final private def setBounds() = {
    xmin = max(0,_focusX - dim)
    xmax = min(r.cols - 1, _focusX + dim)
    ymin = max(0, _focusY - dim)
    ymax = min(r.rows - 1, _focusY + dim)
  }

  def setMask(f:(Int,Int) => Boolean) = {
    hasMask = true
    mask = new CursorMask(d,f)
  }

  /*
   * Get all unmasked cell values covered by the cursor
   * in a sequence. (Non-performant)
   */
  def getAll:Seq[D] = {
    val result = mutable.Set[D]()
    for(v <- this) { result += v }
    result.toSeq
  }

  /*
   * Fold left along all the cell values of the raster
   * which are covered by the cursor and not masked.
   *
   * @param     seed      Seed for the fold operation.
   * @param     f         Function that takes in the seed, or previous computed value,
   *                      and computes a value to be passed into the next iteration.
   */
  def foldLeft(seed:D)(f:(D,D) => D) = {
    var a = seed
    for(v <- this) { a = f(a,v) }
    a
  }

  /*
   * Iterates over all cell values of the raster which
   * are covered by the cursor and not masked.
   *
   * @param     f         Function that receives each cell value.
   */
  def foreach(f: D => Unit):Unit = {
    if(!hasMask) {
      var y = ymin
      var x = 0
      while(y <= ymax) {
        x = xmin
        while(x <= xmax) {
          f(get(x,y))
          x += 1
        }
        y += 1
      }
    } else {
      var y = 0
      while(y < d) {
        mask.foreachX(y) { x =>
          val xRaster = x + (_focusX-dim)
          val yRaster = y + (_focusY-dim)
          if(xmin <= xRaster && xRaster <= xmax && ymin <= yRaster && yRaster <= ymax) {
            f(get(xRaster,yRaster))
          }
        }
        y += 1
      }
    }
  }

  /*
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
   * @param     f         Function that receives each cell value.
   */
  def foreachAdded(f: D => Unit):Unit = {
    if(movement == NoMovement) {
      foreach(f) 
    } else if (movement.isVertical) {
      if(0 <= addedRow && addedRow < r.rows) {
        if(!hasMask) {
          var x = xmin
          while(x <= xmax) {
            f(get(x,addedRow))
            x += 1
          }
        } else {
          mask.foreachX(addedRow-(_focusY-dim)) { x =>
            val xRaster = x+(_focusX-dim)
            if(0 <= xRaster && xRaster <= r.rows) {
              f(get(xRaster,addedRow))
            }
          }
        }
      }        
    } else { // Horizontal
      if(0 <= addedCol && addedCol < r.cols) {
        if(!hasMask) {
          var y = ymin
          while(y <= ymax) {
            f(get(addedCol,y))
            y += 1
          }
        } else {
          if(movement == Left) {
            mask.foreachWestColumn { y =>
              val yRaster = y+(_focusY-dim)
              if(0 <= yRaster && yRaster < r.cols) {
                f(get(addedCol,yRaster))
              }
            }
          } else { // Right
            mask.foreachEastColumn { y =>
              val yRaster = y+(_focusY-dim)
              if(0 <= yRaster && yRaster < r.cols) {
                f(get(addedCol,yRaster))
              }
            }
          }
        }
      }        
    }

    if(hasMask) {
      mask.foreachUnmasked(movement) { (x,y) =>
        val xRaster = x+(_focusX-dim)
        val yRaster = y+(_focusY-dim)
        if(0 <= xRaster && xRaster < r.cols && 0 <= yRaster && yRaster < r.rows) {
          f(get(xRaster,yRaster))
        }
      }
    }
  }

  /*
   * Iterates over all cell values of the raster which
   * are no longer covered by the cursor that were not previously masked
   * not masked, or that were masked when previously unmasked,
   * as part of the last move last move of the cursor.
   *
   * For instance, if move(Movement.Up) is called, then there will
   * potentially be a new row at the bottom of the cursor that is now
   * uncovered by the cursor. These values will be included for the
   * iterations of this function, as well any previously unmasked
   * cell values that were masked as part of the move.
   *
   * @param     f         Function that receives each cell value.
   */
  def foreachRemoved(f: D => Unit):Unit = {
    if(movement == NoMovement) { return }

    if(movement.isVertical) {
      if(0 <= removedRow && removedRow < r.cols) {
        if(!hasMask) {
          var x = xmin
          while(x <= xmax) {
            f(get(x,removedRow))
            x += 1
          }
        } else {
          if(movement == Up) {
            mask.foreachX(d-1) { x =>
              val xRaster = x+(_focusX-dim)
              if(0 <= xRaster && xRaster < r.cols) {
                f(get(xRaster,removedRow))
              }
            }
          }
          else { // Down
            mask.foreachX(0) { x =>
              val xRaster = x+(_focusX-dim)
              if(0 <= xRaster && xRaster < r.cols) {
                f(get(xRaster,removedRow))
              }
            }
          }
        }
      }
    } else { // Horizontal
      if(0 <= removedCol && removedCol < r.rows) {
        if(!hasMask) {
          var y = ymin
          while(y <= ymax) {
            f(get(removedCol,y))
            y += 1
          }
        } else {
          if(movement == Left) {
            mask.foreachEastColumn { y =>
              val yRaster = y+(_focusY-dim)
              if(0 <= yRaster && yRaster < r.cols) {
                f(get(removedCol,yRaster))
              }
            }
          } else { //Right
            mask.foreachWestColumn { y =>
              val yRaster = y+(_focusY-dim)
              if(0 <= yRaster && yRaster < r.cols) {
                f(get(removedCol,yRaster))
              }
            }
          }
        }
      }
    }

    if(hasMask) {
      mask.foreachMasked(movement) { (x,y) =>
        val xRaster = x+(_focusX-dim)
        val yRaster = y+(_focusY-dim)
        if(0 <= xRaster && xRaster < r.cols && 0 <= yRaster && yRaster < r.rows) {
          f(get(xRaster,yRaster))
        }
      }
    }
  }

  def asciiDraw:String = {
    var x = xmin
    var y = ymin
    var result = ""

    val mark = (x:Int, y:Int) => result += " " + getStr(x,y) + " "

    while(y <= ymax) {
      x = xmin
      while(x <= xmax) {
        mark(x,y)
	x += 1
      }
      y += 1
      result += "\n"
    }
    result
  }

  def getStr(x:Int,y:Int):String
}

class IntCursor(r:Raster, dim:Int) extends Cursor[Int](r,dim) {
  def get(x:Int,y:Int) = { raster.get(x,y) }
  def getStr(x:Int,y:Int):String = { "%d".format(get(x,y)) }
}

class DoubleCursor(r:Raster, dim:Int) extends Cursor[Double](r,dim) {
  def get(x:Int,y:Int) = { raster.getDouble(x,y) }
  def getStr(x:Int,y:Int):String = { "%f".format(get(x,y)) }
}

