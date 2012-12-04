package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

sealed trait TraversalStrategy
object TraversalStrategy {
  val ZigZag = new TraversalStrategy { }
  val ScanLine = new TraversalStrategy { }
}
import TraversalStrategy._

/*
 * Focal strategy which moves a Cursor across the raster,
 * allowing a calculation to be done on each cell using the Cursor
 * to determine what neighboring cells are inside the focus's
 * neighborhood, what cells have been added since the last move, and
 * what cells have been removed since the last move.
 */
object CursorStrategy {
  def execute[T](r:Raster,b:IntResultBuilder[T],cursor:IntCursor)
                (calc:IntCursor=>Int):T = 
    execute(r,b,cursor,ZigZag)(calc)

  def execute[T](r:Raster,b:DoubleResultBuilder[T],cursor:DoubleCursor)
                (calc:DoubleCursor=>Double):T =
    execute(r,b,cursor,ZigZag)(calc)

  def execute[T](r:Raster,b:IntResultBuilder[T],cursor:IntCursor,t:TraversalStrategy)
                (calc:IntCursor=>Int):T = {
    val setFunc = () => b.set(cursor.focusX,cursor.focusY,calc(cursor))
    _execute(r,b,cursor,t)(setFunc)
  }

  def execute[T](r:Raster,b:DoubleResultBuilder[T],cursor:DoubleCursor,t:TraversalStrategy)
                (calc:DoubleCursor=>Double):T = {
    val setFunc = () => b.set(cursor.focusX,cursor.focusY,calc(cursor))
    _execute(r,b,cursor,t)(setFunc)
  }

  private def _execute[T](r:Raster,b:Builder[T],cursor:Cursor, t:TraversalStrategy)
                         (setFunc:()=>Unit):T = {
    t match {
      case ScanLine => handleScanLine(r,b,cursor)(setFunc)
      case _ => handleZigZag(r,b,cursor)(setFunc)
    }
  }

  private def handleZigZag[T](r:Raster,b:Builder[T],cursor:Cursor)
                             (setFunc:()=>Unit):T = {
    val maxX = r.cols - 1
    val maxY = r.rows - 1
    var focalX = 0
    var focalY = 0
    var direction = 1

    cursor.centerOn(0, 0)

    while(focalY < r.rows) {
      setFunc()
      focalX += direction
      if(focalX < 0 || maxX < focalX) {
	direction *= -1
	focalY += 1
	focalX += direction
	cursor.move(Movement.Down)
      } else {
        if(direction == 1) { cursor.move(Movement.Right) }
        else { cursor.move(Movement.Left) }
      }
    }

    b.build
  }

  private def handleScanLine[T](r:Raster,b:Builder[T],cursor:Cursor)
                               (setFunc:()=>Unit):T = {
    val maxX = r.cols - 1
    val maxY = r.rows - 1
    var focalX = 0
    var focalY = 0

    cursor.centerOn(0, 0)

    while(focalY < r.rows) {
      setFunc()
      focalX += 1
      if(maxX < focalX) {
	focalY += 1
	focalX = 0
	cursor.centerOn(focalX,focalY)
      } else {
        cursor.move(Movement.Right)
      }
    }

    b.build
  }
}

trait CellwiseCalculator {
  def add(r:Raster,x:Int,y:Int)
  def remove(r:Raster,x:Int,y:Int)
  def reset()
}

trait IntCellwiseCalculator extends CellwiseCalculator {
  def getValue:Int
}

trait DoubleCellwiseCalculator extends CellwiseCalculator {
  def getValue:Double
}

/*
 * Focal strategy that implements a more strict mechanism that informs the user
 * what cells have been added or removed. This strategy is more performant,
 * but can only be used for Square or Circle neighborhoods.
 */ 
object CellwiseStrategy {
  def execute[T](r:Raster,n:Neighborhood,b:IntResultBuilder[T],op:IntCellwiseCalculator):T = {
    val setFunc = (x:Int,y:Int) => b.set(x,y,op.getValue)
    _execute(r,n,b,op)(setFunc)
  }

  def execute[T](r:Raster,n:Neighborhood,b:DoubleResultBuilder[T],op:DoubleCellwiseCalculator):T = {
    val setFunc = (x:Int,y:Int) => b.set(x,y,op.getValue)
    _execute(r,n,b,op)(setFunc)
  }

  private def _execute[T](r:Raster,n:Neighborhood,b:Builder[T],op:CellwiseCalculator)
             (setFunc:(Int,Int) => Unit):T = {
    n match {
      case Square(extent) => _executeSquare(r,b,extent,op)(setFunc)
      case c:Circle => _executeCircle(r,b,c.extent,op)(setFunc)
      case _ => throw new Exception("CellwiseStrategy cannot be used with this neighborhood type.")
    }
  }

  private def _executeSquare[T](r:Raster,b:Builder[T],n:Int, op:CellwiseCalculator)
                      (setFunc:(Int,Int)=>Unit):T = {
    val cols = r.cols
    val rows = r.rows

    var y = 0
    while (y < rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)

      op.reset()
      val xx2 = min(cols, n + 1)
      var yy = yy1
      while (yy < yy2) {
        var xx = 0
        while (xx < xx2) {
          op.add(r, xx, yy)
          xx += 1
        }
        yy += 1
      }

      setFunc(0, y)

      var x = 1
      while (x < cols) {
        val xx1 = x - n - 1
        if (xx1 >= 0) {
          var yy = yy1
          while (yy < yy2) {
            op.remove(r, xx1, yy)
            yy += 1
          }
        }

        val xx2 = x + n
        if (xx2 < cols) {
            var yy = yy1
            while (yy < yy2) {
              op.add(r, xx2, yy)
              yy += 1
            }
          }

        setFunc(x, y)
        x += 1
      }
      y += 1
    }
    b.build
  }

  private def _executeCircle[T](r:Raster,b:Builder[T],n:Int, op:CellwiseCalculator)
                      (setFunc:(Int,Int)=>Unit):T = {
    val cols = r.cols
    val rows = r.rows
    val size = 2 * n + 1

    val xs = new Array[Int](size)
    val nn = n + 0.5
    for (y <- -n to n) xs(y + n) = floor(sqrt(nn * nn - y * y)).toInt

    for (y <- 0 until rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)
    
      for (x <- 0 until cols) {
        op.reset()
    
        for (yy <- yy1 until yy2) {
          val i = (yy - y + n) % size
          val xx1 = max(0, x - xs(i))
          val xx2 = min(cols, x + xs(i) + 1)
          for (xx <- xx1 until xx2) op.add(r, xx, yy)
        }
        setFunc(x, y)
      }
    }

    b.build
  }
}
