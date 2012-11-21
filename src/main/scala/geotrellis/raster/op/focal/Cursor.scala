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
  private val mask = new Array[Boolean](d2)
  private var hasMask = false

  // Values to track the bound of the cursor
  private var xmin = 0
  private var xmax = 0
  private var ymin = 0
  private var ymax = 0

  // Values to track new\old values
  private var newColXMin = 0
  private var newColXMax = 0
  private var oldColXMin = 0
  private var oldColXMax = 0

  private var newRowYMin = 0
  private var newRowYMax = 0 
  private var oldRowYMin = 0
  private var oldRowYMax = 0

  private var hasNewX = false
  private var hasNewY = false
  private var hasOldX = false
  private var hasOldY = false

  // Values to track the focus of the cursor
  private var focusX = 0
  private var focusY = 0
  private var oldFocusX = 0
  private var oldFocusY = 0

  protected def get(x:Int,y:Int):D

  def focus:(Int,Int) = (focusX,focusY)

  def centerOn(x:Int,y:Int) = { 
    oldFocusX = x
    focusX = x
    oldFocusY = y
    focusY = y
    setBounds()
  }

  def moveX(x:Int) = {
    hasNewY = false
    hasOldY = false

    setNewColBounds(x)
    setOldColBounds(x)
    
    oldFocusX = focusX
    focusX += x
    setXBounds()
  }

  def moveY(y:Int) = {
    hasNewX = false
    hasOldX = false

    setNewRowBounds(y)
    setOldRowBounds(y)

    oldFocusY = focusY
    focusY += y
    setYBounds()
  }

  /* Bounds */

  @inline final private def setBounds() = { setXBounds() ; setYBounds() }

  @inline final private def setXBounds() = {
    xmin = max(0,focusX - dim)
    xmax = min(r.cols - 1, focusX + dim)
  }

  @inline final private def setYBounds() = {
    ymin = max(0, focusY - dim)
    ymax = min(r.rows - 1, focusY + dim)
  }

  @inline final private def setNewColBounds(x:Int):Unit = {
    hasNewX = true
    if(x > 0) {
      newColXMin = max(xmax + 1, xmin + x)
      newColXMax = min(r.cols - 1, xmax + x)
    } else {
      newColXMin = max(0, xmin + x)
      newColXMax = min(xmin - 1, xmax - x)
    }
  }

  @inline final private def setOldColBounds(x:Int) = {
    hasOldX = true
    if(x > 0) {
      oldColXMin = max(0,focusX - dim)
      oldColXMax = focusX - dim + x - 1
    } else {
      oldColXMin = focusX + dim
      oldColXMax = min(r.cols - 1, focusX + dim)
    }
  }

  @inline final private def setNewRowBounds(y:Int) = {
    hasNewY = true
    if(y > 0) {
      newRowYMin = max(ymax + 1, ymin + y)
      newRowYMax = min(r.rows - 1, ymax + y)
    } else {
      newRowYMin = max(0, ymin + y)
      newRowYMax = min(ymin - 1, ymax - y)
    }
  }

  @inline final private def setOldRowBounds(y:Int) = {
    hasOldY = true
    if(y > 0) {
      oldRowYMin = max(0,focusY - dim)
      oldRowYMax = focusY - dim + y - 1
    } else {
      oldRowYMin = focusY + dim
      oldRowYMax = min(r.rows - 1, focusY + dim)
    }
  }

  /* Mask */
  def setMask(f:(Int,Int) => Boolean) = {
    hasMask = true
    var x = 0
    var y = 0
    while(x < d) {
      y = 0
      while(y < d) {
        mask(x+d*y) = f(x,y)
        y += 1
      }
      x += 1
    }
  }

  def size:Int = { (xmax - xmin) * (ymax - ymin) }

  def getAll:Seq[D] = {
    val result = mutable.Set[D]()

    var x = xmin
    var y = ymin

    val mark = if(!hasMask) { (x:Int, y:Int) => result += get(x,y) }
               else { (x:Int,y:Int) => if(!mask((x-focusX+dim)  + d*(y-focusY+dim))) { result += get(x,y) } }

    while(y <= ymax) {
      x = xmin
      while(x <= xmax) {
        mark(x,y)
	x += 1
      }
      y += 1
    }
    result.toSeq
  }

  def foldLeft(seed:D)(f:(D,D) => D) = {
    var x = xmin
    var y = ymin
    var a = seed

    val mark = if(!hasMask) { (x:Int, y:Int) => a = f(a,get(x,y)) }
               else { (x:Int,y:Int) => if(!mask((x-focusX+dim)  + d*(y-focusY+dim))) { a = f(a,get(x,y)) } }

    while(y <= ymax) {
      x = xmin
      while(x <= xmax) {
        mark(x,y)
	x += 1
      }
      y += 1
    }
    a
  }

  def foreachNew(f: D => Unit):Unit = {
    val mark = if(!hasMask) { (x:Int, y:Int) => f(get(x,y)) }
               else { (x:Int,y:Int) => if(!mask(getMaskIndex(x,y))) { f(get(x,y)) } }

    if(hasNewX) {
      var x = newColXMin
      var y = ymin
      while(y <= ymax) {
	x = newColXMin
	while(x <= newColXMax) {
          mark(x,y)
	  x += 1
	}
	y += 1
      }
    } else if (hasNewY) {
      var x = xmin
      var y = newRowYMin
      while(x <= xmax) {
	y = newRowYMin
	while(y <= newRowYMax) {
          mark(x,y)
	  y += 1
	}
	x += 1
      }
    }
  }

  def foreachOld(f: D => Unit):Unit = {
    val mark = if(!hasMask) { (x:Int, y:Int) => f(get(x,y)) }
               else { (x:Int,y:Int) => if(!mask((x-oldFocusX+dim)  + d*(y-oldFocusY+dim))) { f(get(x,y)) } }

    if(hasOldX) {
      var x = oldColXMin
      var y = ymin
      while(y <= ymax) {
	x = oldColXMin
	while(x <= oldColXMax) {
          mark(x,y)
	  x += 1
	}
	y += 1
      }
    } else if (hasOldY) {
      var x = xmin
      var y = oldRowYMin
      while(x <= xmax) {
	y = oldRowYMin
	while(y <= oldRowYMax) {
          mark(x,y)
	  y += 1
	}
	x += 1
      }
    }
  }

  def asciiDraw:String = {
    var x = xmin
    var y = ymin
    var result = ""

    val mark = if(!hasMask) { (x:Int, y:Int) => result += " " + getStr(x,y) + " " }
               else { (x:Int,y:Int) => if(!mask(getMaskIndex(x,y))) { result += " " + getStr(x,y) + " " }
                                       else { " X "  } }

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
