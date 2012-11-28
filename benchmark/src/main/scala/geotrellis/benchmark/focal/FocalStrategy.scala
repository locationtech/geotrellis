package geotrellis.benchmark.oldfocal

import scala.math._

import geotrellis._
import geotrellis.raster._

trait Neighborhood { 
  /* How many cells past the focus the bounding box goes (e.g., 1 for 9x9 square) */
  val extent:Int 
}

case class Square(extent:Int) extends Neighborhood {
}

case class Circle(radius:Double) extends Neighborhood {
  val extent = ceil(radius).toInt
}

/**
 * Simple neighborhood that goes through the center of the focus
 * along the x any y axis of the raster
 */
case class Nesw(extent:Int) extends Neighborhood {

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
