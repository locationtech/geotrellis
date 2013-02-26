package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to add values.
 */
object Add {
  /**
   * Adds two integers.
   */
  def apply(x:Op[Int], y:Op[Int]):Op[Int] = logic.Do2(x, y)((x, y) => x + y)
  /**
   * Adds two doubles.
   */
  def apply(x:Op[Double], y:Op[Double])(implicit d: DummyImplicit):Op[Double] = logic.Do2(x, y)((x, y) => x + y)
  /**
   * Add a constant Int value to each cell. See [[AddConstant]]
   */
  def apply(r:Op[Raster], c:Op[Int]) = AddConstant(r, c)
  /**
   * Add a constant Double value to each cell. See [[AddDoubleConstant]]
   */
  def apply(r:Op[Raster], c:Op[Double]) = AddDoubleConstant(r, c)
  /**
   * Add a constant value to each cell. See [[AddConstant]]
   */
  def apply(c:Op[Int], r:Op[Raster])(implicit d: DummyImplicit) = AddConstant(r, c)
  /**
   * Add a constant Double value to each cell. See [[AddDoubleConstant]]
   */
  def apply(c:Op[Double], r:Op[Raster])(implicit d: DummyImplicit) = AddDoubleConstant(r,c)
  /**
   * Add the values of each cell in each raster. See [[AddRasters]]
   */
  def apply(rs:Op[Raster]*) = AddRasters(rs:_*)
}

/**
 * Add a constant integer value to each cell.
 */
case class AddConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ + c)(_ + c))
})

/**
 * Add a constant double value to each cell.
 */
case class AddDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet({i:Int => (i + c).toInt})(_ + c))
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
