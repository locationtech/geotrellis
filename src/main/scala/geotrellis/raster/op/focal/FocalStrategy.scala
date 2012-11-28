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
  def execute[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],cursor:Cursor[D])
                                        (calc:Cursor[D]=>D):T = 
      execute(r,b,cursor,ZigZag)(calc)

  def execute[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],cursor:Cursor[D], t:TraversalStrategy)
                                        (calc:Cursor[D]=>D):T = {
    t match {
      case ScanLine => handleScanLine(r,b,cursor)(calc)
      case _ => handleZigZag(r,b,cursor)(calc)
    }
  }

  private def handleZigZag[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],cursor:Cursor[D])
                                        (calc:Cursor[D]=>D):T = {
    val maxX = r.cols - 1
    val maxY = r.rows - 1
    var focalX = 0
    var focalY = 0
    var direction = 1

    cursor.centerOn(0, 0)

    while(focalY < r.rows) {
      b.set(focalX, focalY, calc(cursor))
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

  private def handleScanLine[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],cursor:Cursor[D])
                                        (calc:Cursor[D]=>D):T = {
    val maxX = r.cols - 1
    val maxY = r.rows - 1
    var focalX = 0
    var focalY = 0

    cursor.centerOn(0, 0)

    while(focalY < r.rows) {
      b.set(focalX, focalY, calc(cursor))
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

trait CellwiseCalculator[@specialized(Int,Double)D] {
  def add(r:Raster,x:Int,y:Int)
  def remove(r:Raster,x:Int,y:Int)
  def reset()
  def getValue:D
}

/*
 * Focal strategy that implements a more strict mechanism that informs the user
 * what cells have been added or removed. This strategy is more performant,
 * but can only be used for Square or Circle neighborhoods.
 */ 
object CellwiseStrategy {
  def execute[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],n:Neighborhood)
                                        (op:CellwiseCalculator[D]):T = {
    n match {
      case Square(extent) => executeSquare(r,b,extent)(op)
      case c:Circle => executeCircle(r,b,c.extent)(op)
      case _ => throw new Exception("CellwiseStrategy cannot be used with this neighborhood type.")
    }
  }

  def executeSquare[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],n:Int)
                                              (op:CellwiseCalculator[D]):T = {
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

      b.set(0, y, op.getValue)

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

        b.set(x, y, op.getValue)
        x += 1
      }
      y += 1
    }
    b.build
  }

  def executeCircle[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],n:Int)
                                              (op:CellwiseCalculator[D]):T = {
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
        b.set(x, y, op.getValue)
      }
    }

    b.build
  }
}
