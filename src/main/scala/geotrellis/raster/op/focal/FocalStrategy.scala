package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

sealed trait TraversalStrategy
object TraversalStrategy {
  val ZigZag = new TraversalStrategy { }
  val ScanLine = new TraversalStrategy { }
  val SpiralZag = new TraversalStrategy { }
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
  def execute(r:Raster,cursor:Cursor)
                (c:(Raster,Cursor)=>Unit):Unit = 
    execute(r,cursor,ZigZag)(c)

  def execute(r:Raster,cursor:Cursor,t:TraversalStrategy)
                (c:(Raster,Cursor)=>Unit):Unit = {
    t match {
      case ScanLine => handleScanLine(r,cursor)(c)
      case SpiralZag => handleSpiralZag(r,cursor)(c)
      case _ => handleZigZag(r,cursor)(c)
    }
  }

  private def handleSpiralZag(r:Raster,cursor:Cursor) 
                             (c:(Raster,Cursor)=>Unit) = {
    var xmin = 0
    var ymin = 0
    var xmax = r.cols - 1
    var ymax = r.rows - 1
    var x = 0
    var y = 0
    var xdirection = 1
    var ydirection = 1
    var done = false
    var zagTime = false

    cursor.centerOn(0,0)
    
    // Spiral around the raster.
    // Once we get down with dealing with borders,
    // turn on fast mode for the cursor.
    while(!(done || zagTime)) {
      //Move right across top
      while(x < xmax) {
        c(r,cursor)
        cursor.move(Movement.Right)
        x += 1
      }
      // Move down along right edge
      while(y < ymax) {
        c(r,cursor)
        cursor.move(Movement.Down)
        y += 1
      }
      //Move left across bottom
      while(x > xmin) {
        c(r,cursor)
        cursor.move(Movement.Left)
        x -= 1
      }
      // Move up along left edge
      while(y > ymin+1) {
        c(r,cursor)
        cursor.move(Movement.Up)
        y -= 1
      }
      c(r,cursor)
      ymin += 1
      ymax -= 1
      xmin += 1
      xmax -= 1

      if(ymin == ymax || xmin == xmax) { 
        done = true 
      } else {
        cursor.move(Movement.Right)
        x += 1
        if(x - cursor.dim >= 0) {
          zagTime = true
        }
      }
    }

    var direction = 1

    // Now zig zag across interior.
    while(y <= ymax) {
      c(r,cursor)
      x += direction
      if(x < xmin || xmax < x) {
	direction *= -1
	y += 1
	x += direction
	cursor.move(Movement.Down)
      } else {
        if(direction == 1) { cursor.move(Movement.Right) }
        else { cursor.move(Movement.Left) }
      }
    }
  }

  private def handleZigZag(r:Raster,cursor:Cursor)
                             (c:(Raster,Cursor)=>Unit) = {
    val maxX = r.cols - 1
    val maxY = r.rows - 1
    var x = 0
    var y = 0
    var direction = 1

    cursor.centerOn(0, 0)

    while(y < r.rows) {
      c(r,cursor)
      x += direction
      if(x < 0 || maxX < x) {
	direction *= -1
	y += 1
	x += direction
	cursor.move(Movement.Down)
      } else {
        if(direction == 1) { cursor.move(Movement.Right) }
        else { cursor.move(Movement.Left) }
      }
    }
  }

  private def handleScanLine(r:Raster,cursor:Cursor)
                               (c:(Raster,Cursor)=>Unit) = {
    val maxX = r.cols - 1
    val maxY = r.rows - 1
    var x = 0
    var y = 0

    cursor.centerOn(0, 0)

    while(y < r.rows) {
      c(r,cursor)
      x += 1
      if(maxX < x) {
	y += 1
	x = 0
	cursor.centerOn(x,y)
      } else {
        cursor.move(Movement.Right)
      }
    }
  }
}

trait CellwiseCalculator {
  def add(r:Raster,x:Int,y:Int)
  def remove(r:Raster,x:Int,y:Int)
  def reset()
  def setValue(x:Int,y:Int)
}

/*
 * Focal strategy that implements a more strict mechanism that informs the user
 * what cells have been added or removed. This strategy is more performant,
 * but can only be used for Square or Circle neighborhoods.
 */ 
object CellwiseStrategy {
  def execute(r:Raster,n:Neighborhood,calc:CellwiseCalculator):Unit = {
    n match {
      case Square(extent) => _executeSquare(r,extent,calc)
      case c:Circle => _executeCircle(r,c.extent,calc)
      case _ => throw new Exception("CellwiseStrategy cannot be used with this neighborhood type.")
    }
  }

  private def _executeSquare[T](r:Raster,n:Int, calc:CellwiseCalculator) = {
    val cols = r.cols
    val rows = r.rows

    var y = 0
    while (y < rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)

      calc.reset()
      val xx2 = min(cols, n + 1)
      var yy = yy1
      while (yy < yy2) {
        var xx = 0
        while (xx < xx2) {
          calc.add(r, xx, yy)
          xx += 1
        }
        yy += 1
      }

      calc.setValue(0, y)

      var x = 1
      while (x < cols) {
        val xx1 = x - n - 1
        if (xx1 >= 0) {
          var yy = yy1
          while (yy < yy2) {
            calc.remove(r, xx1, yy)
            yy += 1
          }
        }

        val xx2 = x + n
        if (xx2 < cols) {
            var yy = yy1
            while (yy < yy2) {
              calc.add(r, xx2, yy)
              yy += 1
            }
          }

        calc.setValue(x, y)
        x += 1
      }
      y += 1
    }
  }

  private def _executeCircle[T](r:Raster,n:Int,calc:CellwiseCalculator) = {
    val cols = r.cols
    val rows = r.rows
    val size = 2 * n + 1

    val xs = new Array[Int](size)
    val nn = n + 0.5
    for (y <- -n to n) xs(y + n) = floor(sqrt(n * n - y * y)).toInt

    for (y <- 0 until rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)
    
      for (x <- 0 until cols) {
        calc.reset()
    
        for (yy <- yy1 until yy2) {
          val i = (yy - y + n) % size
          val xx1 = max(0, x - xs(i))
          val xx2 = min(cols, x + xs(i) + 1)
          for (xx <- xx1 until xx2) calc.add(r, xx, yy)
        }
        calc.setValue(x, y)
      }
    }
  }
}
