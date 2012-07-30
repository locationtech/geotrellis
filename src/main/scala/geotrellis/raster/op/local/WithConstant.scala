package geotrellis.raster.op.local

import geotrellis._
import geotrellis._
import geotrellis.process._

import scala.math.{max, min, pow}

/**
 * Add a constant value to each cell.
 */
case class AddConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ + c)(_ + c))
})

/**
 * Subtract a constant value from each cell.
 */
case class SubtractConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ - c)(_ - c))
})

/**
 * Subtract the value of each cell by a constant.
 */
case class SubtractConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(c - _)(c - _))
})

/**
 * Multiply each cell by a constant.
 */
case class MultiplyConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ * c)(_ * c))
})

/**
 * Multiply each cell by a constant (double).
 * 
 * Create with MultiplyConstant(raster, doubleValue).
 */
case class MultiplyDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ * c.toInt)(_ * c))
})

/**
 * Divide each cell by a constant value.
 */
case class DivideConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ / c)(_ / c))
})

case class DivideDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ / c.toInt)(_ / c))
})


/**
 * For each cell, divide a constant value by that cell's value.
 */
case class DivideConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(c / _)(c / _))
})


case class DivideDoubleConstantBy(c:Op[Double], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.dualMapIfSet(c.toInt / _)(c / _))
})

/**
 * Set each cell to a constant number or the corresponding cell value, whichever is highest.
 */
object MaxConstant {
  def apply(r:Op[Raster], c:Op[Int]) = Max(r, c)
}

/**
 * Set each cell to a constant or its existing value, whichever is lowest.
 */
object MinConstant {
  def apply(r:Op[Raster], c:Op[Int]) = Min(r, c)
}

/**
 * Raise each cell to the cth power.
 */
case class PowConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c) ({
  (r,c) => Result(r.dualMapIfSet(pow(_, c).toInt)(pow(_, c)))
})

case class PowDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r,c) ({
  (r,c) => Result(r.dualMapIfSet(pow(_, c).toInt)(pow(_, c)))
})
