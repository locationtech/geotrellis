package geotrellis.raster.op.local

import geotrellis._

object Add {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => x + y)
  def apply(r:Op[Raster], c:Op[Int]) = AddConstant(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = AddConstant2(c, r)
  def apply(rs:Op[Raster]*) = AddRasters(rs:_*)
}

/**
 * Add a constant value to each cell.
 */
case class AddConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ + c)(_ + c))
})

/**
 * Add a constant value to each cell.
 */
case class AddConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(_ + c)(_ + c))
})

/**
 * Add the values of each cell in each raster.
 */
case class AddRasters(rs:Op[Raster]*) extends MultiLocal {
  final def ops = rs.toArray

  final def handle(a:Int, b:Int) = if (a == NODATA) b else if (b == NODATA) a else a + b

  final def handleDouble(a:Double, b:Double) =
    if (java.lang.Double.isNaN(a)) b else if (java.lang.Double.isNaN(b)) a else a + b
}

/**
 * Add the values of each cell in each raster.
 */
case class AddArray(op:Op[Array[Raster]]) extends MultiLocalArray {
  final def handle(a:Int, b:Int) = if (a == NODATA) b else if (b == NODATA) a else a + b

  final def handleDouble(a:Double, b:Double) =
    if (java.lang.Double.isNaN(a)) b else if (java.lang.Double.isNaN(b)) a else a + b
}
