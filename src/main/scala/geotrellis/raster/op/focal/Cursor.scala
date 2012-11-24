package geotrellis.raster.op.focal

import scala.collection.mutable
import scala.math.{min,max}

import geotrellis._

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


abstract class Cursor[@specialized(Int,Double) D](r:Raster, dim:Int) {
  protected val raster = r

  val distanceFromCenter = dim
  private val d = 2*dim + 1
  val d2 = d*d

  @inline final private def getMaskIndex(x:Int,y:Int) = (x-focusX+dim)  + d*(y-focusY+dim)
  private var mask:CursorMask = null
  private var hasMask = false

  // Values to track the bound of the cursor
  private var xmin = 0
  private var xmax = 0
  private var ymin = 0
  private var ymax = 0
  private var oldxmin = 0
  private var oldxmax = 0
  private var oldymin = 0
  private var oldymax = 0

  // Values to track new\old values
  private var hasNewCol = false
  private var newCol = 0
  private var hasOldCol = false
  private var oldCol = 0
  private var movedLeft = false
  private var movedRight = false

  private var hasNewRow = false
  private var newRow = 0
  private var hasOldRow = false
  private var oldRow = 0
  private var movedUp = false
  private var movedDown = false
  private var moved = false

  // Values to track the focus of the cursor
  private var focusX = 0
  private var focusY = 0
  private var oldFocusX = 0
  private var oldFocusY = 0

  protected def get(x:Int,y:Int):D

  def focus:(Int,Int) = (focusX,focusY)

  def centerOn(x:Int,y:Int) = { 
    hasNewRow = false
    hasOldRow = false
    hasNewCol = false
    hasOldCol = false
    moved = false
    oldFocusX = x
    focusX = x
    oldFocusY = y
    focusY = y
    setBounds()
  }

  def moveRight() = {
    moved = true
    hasNewRow = false
    hasOldRow = false
    movedUp = false
    movedDown = false
    movedLeft = false
    movedRight = true

    // track the new column
    newCol = xmax + 1
    hasNewCol = newCol < r.cols

    // track the old column
    oldCol = focusX - dim
    hasOldCol = oldCol >= 0

    oldFocusX = focusX
    focusX += 1
    setXBounds()
  }

  def moveLeft() = {
    moved = true
    hasNewRow = false
    hasOldRow = false
    movedUp = false
    movedDown = false
    movedRight = false
    movedLeft = true

    // track the new column
    newCol = xmin - 1
    hasNewCol = newCol >= 0

    // track the old column
    oldCol = focusX + dim
    hasOldCol = oldCol < r.cols

    oldFocusX = focusX
    focusX -= 1
    setXBounds()
  }

  def moveDown() = {
    moved = true
    hasNewCol = false
    hasOldCol = false
    movedRight = false
    movedLeft = false
    movedUp = false
    movedDown = true

    newRow = ymax + 1
    hasNewRow = newRow < r.rows

    oldRow = focusY - dim
    hasOldRow = oldRow >= 0

    oldFocusY = focusY
    focusY += 1
    setYBounds()
  }

  def moveUp() = {
    moved = true
    hasNewCol = false
    hasOldCol = false
    movedRight = false
    movedLeft = false
    movedDown = false
    movedUp = true

    newRow = ymin - 1
    hasNewRow = newRow >= 0

    oldRow = focusY + dim
    hasOldRow = oldRow < r.rows

    oldFocusY = focusY
    focusY -= 1
    setYBounds()
  }

  /* Bounds */

  @inline final private def setBounds() = { setXBounds() ; setYBounds() }

  @inline final private def setXBounds() = {
    oldxmin = xmin
    oldxmax = xmax
    xmin = max(0,focusX - dim)
    xmax = min(r.cols - 1, focusX + dim)
  }

  @inline final private def setYBounds() = {
    oldymin = ymin
    oldymax = ymax
    ymin = max(0, focusY - dim)
    ymax = min(r.rows - 1, focusY + dim)
  }

  def setMask(f:(Int,Int) => Boolean) = {
    hasMask = true
    mask = new CursorMask(d,f)
  }

  def size:Int = { (xmax - xmin) * (ymax - ymin) }

  def getAll:Seq[D] = {
    val result = mutable.Set[D]()
    var y = ymin

    if(!hasMask) {
      var x = xmin
      while(y <= ymax) {
        x = xmin
        while(x <= xmax) {
          result += get(x,y) 
	  x += 1
        }
        y += 1
      }
    } else {
      while(y <= ymax) {
        mask.foreachX(y-ymin,dim-(focusX-xmin),xmax-xmin) { x =>
                                                              result += get(x+xmin,y)
                                                          }
        y += 1
      }
    }
    result.toSeq
  }

  def foldLeft(seed:D)(f:(D,D) => D) = {
    var a = seed
    var y = ymin

    if(!hasMask) {
      var x = xmin
      while(y <= ymax) {
        x = xmin
        while(x <= xmax) {
          a = f(a,get(x,y)) 
	  x += 1
        }
        y += 1
      }
    } else {
      while(y <= ymax) {
        mask.foreachX(y-ymin,dim - (focusX-xmin), xmax - xmin) { x =>
                                a = f(a, get(x + xmin, y))
                              }
        y += 1
      }
    }
    a
  }

  def foreach(f: D => Unit):Unit = {
    var y = ymin

    if(!hasMask) {
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
      while(y <= ymax) {
        mask.foreachX(y-ymin, dim - (focusX-xmin), xmax - xmin) { x =>
          f(get(x + xmin,y))
        }
        y += 1
      }
    }
  }

  def foreachNew(f: D => Unit):Unit = {
    if(moved) {
      if(hasNewCol) {
        if(!hasMask) {
          var y = ymin
          while(y <= ymax) {
            f(get(newCol,y))
	    y += 1
          }
        }
        else {
          var y = ymin
          while(y <= ymax) {
            if(!mask.maskFunc(newCol-xmin,y-ymin)) { f(get(newCol,y)) }
            y += 1
          }
          if(movedRight) { 
            mask.foreachUnmaskedRight(dim-(focusX-xmin),xmax-xmin,
                                      dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                        f(get(x+xmin,y+ymin))
                                                                  }
          } else if(movedLeft) {
            mask.foreachUnmaskedLeft(dim-(focusX-xmin),xmax-xmin,
                                     dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                       f(get(x+xmin,y+ymin))
                                                                 }

          }
        }
      } else if (hasNewRow) {
        if(!hasMask) {
          var x = xmin
          while(x <= xmax) {
            f(get(x,newRow))
	    x += 1
          }
        } else {
          mask.foreachX(newRow-ymin, dim - (focusX-xmin),xmax - xmin) { x =>
            f(get(x+xmin,newRow))
                                                                     }
          if(movedUp) { 
            mask.foreachUnmaskedUp(dim-(focusX-xmin),xmax-xmin,
                                   dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                     f(get(x+xmin,y+ymin))
                                                               }
          } else if(movedDown) {
            mask.foreachUnmaskedDown(dim-(focusX-xmin),xmax-xmin,
                                     dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                       f(get(x+xmin,y+ymin))
                                                                 }

          }
        }
      } 
    } else  {
      // All of cursor is new.
      foreach(f)
    }
  }

  def foreachOld(f: D => Unit):Unit = {
    if(!moved) { return }

    val mark = (x:Int, y:Int) => f(get(x,y))

    if(hasOldCol) {
      if(!hasMask) {
        var y = ymin
        while(y <= ymax) {
          f(get(oldCol,y))
	  y += 1
        }
      } else {
        var y = ymin        
        while(y <= ymax) {
          if(!mask.maskFunc(oldCol-oldxmin,y-ymin)) { f(get(oldCol,y)) }
          y += 1
        }
        if(movedRight) { 
          mask.foreachMaskedRight(dim-(focusX-xmin),xmax-xmin,
                                    dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                      f(get(x+xmin,y+ymin))
                                                                }
        } else if(movedLeft) {
          mask.foreachMaskedLeft(dim-(focusX-xmin),xmax-xmin,
                                   dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                     f(get(x+xmin,y+ymin))
                                                               }
        }
      }
    } else if (hasOldRow) {
      if(!hasMask) {
        var x = xmin
        while(x <= xmax) {
          f(get(x,oldRow))
	  x += 1
        }
      } else {
        mask.foreachX(oldRow-oldymin, dim - (focusX-xmin),xmax - xmin) { x =>
                                                              f(get(x+xmin,oldRow))
                                                                   }
        if(movedUp) { 
          mask.foreachMaskedUp(dim-(focusX-xmin),xmax-xmin,
                                 dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                   f(get(x+xmin,y+ymin))
                                 }
        } else if(movedDown) {
          mask.foreachMaskedDown(dim-(focusX-xmin),xmax-xmin,
                                   dim-(focusY-ymin),ymax-ymin) { (x,y) =>
                                     f(get(x+xmin,y+ymin))
                                   }

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

