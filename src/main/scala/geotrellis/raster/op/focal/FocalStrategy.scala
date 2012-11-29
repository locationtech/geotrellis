package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

/*
 * Focal strategy which moves a Cursor across the raster,
 * allowing a calculation to be done on each cell using the Cursor
 * to determine what neighboring cells are inside the focus's
 * neighborhood, what cells have been added since the last move, and
 * what cells have been removed since the last move.
 */
object CursorStrategy {
  def execute[T,@specialized(Int,Double)D](r:Raster,b:ResultBuilder[T,D],cursor:Cursor[D])
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

trait FocalStrategy2[@specialized(Int,Double) D] {
 
  def execute(r:Raster, b:RasterBuilder[D])(calc:Cursor[D] => D):Raster = 
    execute(r,b,1)(calc)

  def execute(r:Raster, b:RasterBuilder[D], dim:Int)(calc:Cursor[D] => D):Raster = {
    val maxX = r.cols - 1
    val maxY = r.rows - 1
    var focalX = 0
    var focalY = 0
    var direction = 1
    val cursor = createCursor(r,dim)

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

  def createCursor(r:Raster,dim:Int):Cursor[D]

  def rasterValue(r:Raster,x:Int,y:Int):D
}

object IntFocalStrategy extends FocalStrategy2[Int] {
  def createCursor(r:Raster,dim:Int) = new IntCursor(r,dim)
  def rasterValue(r:Raster, x:Int, y:Int) = r.get(x,y)
}

object DoubleFocalStrategy extends FocalStrategy2[Double] {
  def createCursor(r:Raster,dim:Int) = new DoubleCursor(r,dim)
  def rasterValue(r:Raster, x:Int, y:Int) = r.getDouble(x,y)
}

/**
 * An algorithm that iterates over a raster and performs a focal operation, using
 * a neighborhood calculation that is passed in.
 */
trait FocalStrategy[@specialized(Int,Double) D] {
  def relativeBounds:(Int, Int, Int, Int)
  def handle[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster
}

object FocalStrategy {
  def get[@specialized(Int,Double) D](strategyType: FocalStrategyType, nhType: Neighborhood): FocalStrategy[D] = {
    nhType match {
      case Square(ext) => 
	SquareStrategy[D](ext, strategyType)
      case c: Circle =>
	CircleStrategy[D](c.extent, strategyType)
      case t: Nesw =>
	NeswStrategy[D](strategyType)
    }
  }
}

case class NeswStrategy[@specialized(Int,Double) D](strategyType: FocalStrategyType) extends FocalStrategy[D] {
  def relativeBounds = (-1, -1, 1, 1)

  def handle[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = strategyType match {
    case _ => handleDefault(r, data, makeCalc)
  }

  def handleDefault[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = {
    val calc = makeCalc()
    val cols = r.cols
    val rows = r.rows
    var y = 0
    while (y < rows) {
      var x = 0
      while (x < cols) {
        calc.clear()
        calc.center(x, y, r)
        calc.add(x, y, r)
        if (x > 0) calc.add(x - 1, y, r)
        if (x < cols - 1) calc.add(x + 1, y, r)
        if (y > 0) calc.add(x, y - 1, r)
        if (y < rows - 1) calc.add(x, y + 1, r)
        data.store(x, y, calc.getResult)
        x += 1
      }
      y += 1
    }
    data.get()
  }
}

case class SquareStrategy[@specialized(Int,Double) D](n:Int, strategyType: FocalStrategyType) extends FocalStrategy[D] {
  def relativeBounds = (-n, -n, n, n)

  def handle[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = strategyType match {
    case Aggregated => handleAggregated(r, data, makeCalc)
    case Sliding => handleSliding(r, data, makeCalc)
    case _ => handleDefault(r, data, makeCalc)
  }

  def handleDefault[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = {
    val calc = makeCalc()
    val cols = r.cols
    val rows = r.rows
    for (y <- 0 until rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)

      for (x <- 0 until cols) {
        calc.clear()
        calc.center(x, y, r)

        val xx1 = max(0, x - n)
        val xx2 = min(cols, x + n + 1)
        for (yy <- yy1 until yy2; xx <- xx1 until xx2) calc.add(xx, yy, r)
        data.store(x, y, calc.getResult)
      }
    }
    data.get()
  }

  def handleAggregated[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = {
    val calc = makeCalc()
    val cols = r.cols
    val rows = r.rows

    var y = 0
    while (y < rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)

      calc.clear()
      val xx2 = min(cols, n + 1)
      var yy = yy1
      while (yy < yy2) {
        var xx = 0
        while (xx < xx2) {
          calc.add(xx, yy, r)
          xx += 1
        }
        yy += 1
      }

      data.store(0, y, calc.getResult)

      var x = 1
      while (x < cols) {
        val xx1 = x - n - 1
        if (xx1 >= 0) {
          var yy = yy1
          while (yy < yy2) {
            calc.remove(xx1, yy, r)
            yy += 1
          }
        }

        val xx2 = x + n
        if (xx2 < cols) {
            var yy = yy1
            while (yy < yy2) {
              calc.add(xx2, yy, r)
              yy += 1
            }
          }

        data.store(x, y, calc.getResult)
        x += 1
      }
      y += 1
    }
    data.get()
  }

  def handleSliding[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = {
    val calc = makeCalc()
    val cols = r.cols
    val rows = r.rows
    val colBound = cols - n
    var currRow = 0
    val nhMaxX = min(cols, n + 1)

    while (currRow < rows) {
      val nhMinY = max(0, currRow - n)
      val nhMaxY = min(rows, currRow + n + 1)

      // Fill out first neighborhood in row
      calc.clear()
      calc.center(0, currRow, r)
      var nhY = nhMinY
      while (nhY < nhMaxY) {
        var nhX = 0
        while (nhX < nhMaxX) { calc.add(nhX, nhY, r); nhX += 1 }
        nhY += 1
      }
      data.store(0, currRow, calc.getResult)

      // Calculate by centering across this row,
      // setting only the eastern edge as the calculation
      // should be caching the columns of the neighborhood 
      // and moving them west as the calculation is
      // recentered.
      var x = 1
      while (x < colBound) {
        calc.center(x, currRow, r)
        var nhY = nhMinY
        while (nhY < nhMaxY) { calc.add(x + n, nhY, r); nhY += 1 }
        data.store(x, currRow, calc.getResult)
        x += 1
      }

      // This part takes care of when the neighborhood is being cut off
      // at the eastern edge.
      while (x < cols) {
        calc.center(x, currRow, r)
        var nhY = nhMinY
        while (nhY < nhMaxY) { calc.remove(x + n, nhY, r); nhY += 1 }
        data.store(x, currRow, calc.getResult)
        x += 1
      }
      currRow += 1
    }
    data.get()
  }
}

case class CircleStrategy[@specialized(Int,Double) D](n:Int, strategyType:FocalStrategyType) extends FocalStrategy[D] {
  def relativeBounds = (-n, -n, n, n)

  def handle[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = strategyType match {
    case _ => handleDefault(r, data, makeCalc)
  }

  def handleDefault[C <: FocalCalculation[D]](r:Raster, data: FocalOpData[D], makeCalc: () => C): Raster = {
    val calc = makeCalc()
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
        calc.clear()
        calc.center(x, y, r)
    
        for (yy <- yy1 until yy2) {
          val i = (yy - y + n) % size
          val xx1 = max(0, x - xs(i))
          val xx2 = min(cols, x + xs(i) + 1)
          for (xx <- xx1 until xx2) calc.add(xx, yy, r)
        }
        data.store(x, y, calc.getResult)
      }
    }
    data.get()
  }
}
