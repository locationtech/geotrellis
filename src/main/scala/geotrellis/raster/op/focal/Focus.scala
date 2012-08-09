package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

trait Focus {
  def relativeBounds:(Int, Int, Int, Int)
  def handle[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A
}

case class Square(n:Int) extends Focus {
  def relativeBounds = (-n, -n, n, n)

  def handle[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A = c.focalType match {
    case Aggregated => handleAggregated(r, c)
    case _ => handleDefault(r, c)
  }

  def handleDefault[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A = {
    val cc = c.makeCell()
    val cols = r.cols
    val rows = r.rows
    for (y <- 0 until rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)

      for (x <- 0 until cols) {
        cc.clear()
        cc.center(x, y)

        val xx1 = max(0, x - n)
        val xx2 = min(cols, x + n + 1)
        for (yy <- yy1 until yy2; xx <- xx1 until xx2) cc.add(xx, yy, r)
        c.store(x, y, cc)
      }
    }
    c.get()
  }

  def getColumn[A, C <: Cell[C]](r:Raster, c:Context[A, C], xx:Int, yy1:Int, yy2:Int) = {
    val cc = c.makeCell()
    for (yy <- yy1 until yy2) cc.add(xx, yy, r)
    cc
  }

  def emptyColumn[A, C <: Cell[C]](c:Context[A, C]) = c.makeCell()

  def combineColumns[A, C <: Cell[C]](r:Raster, c:Context[A, C], columns:Array[Int], size:Int) = {
    val cc = c.makeCell()
    var i = 0
    while (i < size) { cc.add(columns(i)); i += 1 }
    cc
  }

  def handleColumnar[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A = {
    val cc = c.makeCell()
    val cols = r.cols
    val rows = r.rows
    val size = 2 * n + 1

    for (y <- 0 until rows) {
      val columns = new Array[Int](size)

      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)
      for (xx <- 0 until min(cols, n + 1)) columns(xx) = getColumn(r, c, xx, yy1, yy2).get()
      c.store(0, y, combineColumns(r, c, columns, size))

      for (x <- 1 until cols) {
        val i = x % size

        val xx1 = x - n - 1
        val xx2 = x + n

        val old = columns(i)
        columns(i) = (if (xx2 < cols) getColumn(r, c, xx2, yy1, yy2) else c.makeCell()).get()
        c.store(x, y, combineColumns(r, c, columns, size))
      }
    }
    c.get()
  }

  def handleAggregated[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A = {
    val cc = c.makeCell()
    val cols = r.cols
    val rows = r.rows

    for (y <- 0 until rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)

      cc.clear()
      for (yy <- yy1 until yy2; xx <- 0 until min(cols, n + 1)) cc.add(xx, yy, r)
      c.store(0, y, cc)

      for (x <- 1 until cols) {
        val xx1 = x - n - 1
        if (xx1 >= 0) for (yy <- yy1 until yy2) cc.remove(xx1, yy, r)

        val xx2 = x + n
        if (xx2 < cols) for (yy <- yy1 until yy2) cc.add(xx2, yy, r)

        c.store(x, y, cc)
      }
    }
    c.get()
  }
}

case class Circle(n:Int) extends Focus {
  def relativeBounds = (-n, -n, n, n)

  def handle[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A = c.focalType match {
    case _ => handleDefault(r, c)
  }

  def handleDefault[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A = {
    val cc = c.makeCell()
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
        cc.clear()
        cc.center(x, y)
    
        for (yy <- yy1 until yy2) {
          val i = (yy - y + n) % size
          val xx1 = max(0, x - xs(i))
          val xx2 = min(cols, x + xs(i) + 1)
          for (xx <- xx1 until xx2) cc.add(xx, yy, r)
        }
        c.store(x, y, cc)
      }
    }
    c.get()
  }

  def handleAggregated[A, C <: Cell[C]](r:Raster, c:Context[A, C]):A = {
    val cc = c.makeCell()
    val cols = r.cols
    val rows = r.rows

    for (y <- 0 until rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n + 1)

      cc.clear()
      for (yy <- yy1 until yy2; xx <- 0 until min(cols, n + 1)) cc.add(xx, yy, r)
      c.store(0, y, cc)

      for (x <- 1 until cols) {
        val xx1 = x - n - 1
        if (xx1 >= 0) for (yy <- yy1 until yy2) cc.remove(xx1, yy, r)

        val xx2 = x + n
        if (xx2 < cols) for (yy <- yy1 until yy2) cc.add(xx2, yy, r)

        c.store(x, y, cc)
      }
    }
    c.get()
  }
}
