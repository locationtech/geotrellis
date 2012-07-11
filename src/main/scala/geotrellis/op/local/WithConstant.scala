package geotrellis.op.local

import geotrellis._
import geotrellis.op._
import geotrellis.process._

import scala.math.{max, min, pow}

/**
 * Add a constant value to each cell.
 */
case class AddConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c)({
  (r,c) => Result(r.map( z => if (z != NODATA) z + c else NODATA))
}) 

/**
 * Subtract a constant value from each cell.
 */
case class SubtractConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c)({
  (r,c) => Result(r.map( z => if (z != NODATA) z - c else NODATA))
})

/**
 * Subtract the value of each cell by a constant.
 */
case class SubtractConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c,r)({
  (c,r) => Result(r.map( z => if (z != NODATA) c - z else NODATA))
})


/**
 * Multiply each cell by a constant.
 */
case class MultiplyConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c)({
  (r,c) => Result(r.map( z => if (z != NODATA) z * c else NODATA))
})

/**
 * Multiply each cell by a constant (double).
 * 
 * Create with MultiplyConstant(raster, doubleValue).
 */
case class MultiplyDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r,c)({
  (r,c) => Result(r.map( z => if ( z != NODATA) (z * c).toInt else NODATA))
})


/**
 * Divide each cell by a constant value.
 */
case class DivideConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c)({
  (r,c) => Result(r.map( z => if (z != NODATA) z / c else NODATA))
})

case class DivideDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r,c)({
  (r,c) => Result(r.map( z => if (z != NODATA) (z / c).toInt else NODATA))
})


/**
 * For each cell, divide a constant value by that cell's value.
 */
case class DivideConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c,r)({
  (_c:Int,r) => Result(r.map ( (z:Int) => if (z != NODATA) (_c / z) else NODATA))
})


case class DivideDoubleConstantBy(c:Op[Double], r:Op[Raster]) extends Op2(c,r) ({
  (_c:Double,r) => Result(r.map ( z => if (z != NODATA) (_c / z).toInt else NODATA))
})

/**
 * Bitmask each cell by a constant value.
 */
case class Bitmask(r:Op[Raster], c:Op[Int]) extends Op2(r,c) ({
  (r,c) => Result(r.map ( z => if (z != NODATA) z & c else NODATA))
})

/**
  * Set each cell to a constant number or the corresponding cell value, whichever is highest.
  */
case class MaxConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c) ({
  (r,c) => Result(r.map ( z => if (z != NODATA) max(z, c) else NODATA))
})

/**
  * Set each cell to a constant or its existing value, whichever is lowest.
  */
case class MinConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c) ({
  (r,c) => Result(r.map (z => if (z != NODATA) min(z,c) else NODATA))
})

/**
 * Raise each cell to the cth power.
 */
case class PowConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c) ({
  (r,c) => Result(r.map (z => if (z != NODATA) pow(z,c).toInt else NODATA))
})

case class PowDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r,c) ({
  (r,c) => Result(r.map (z => if (z != NODATA) pow(z, c).toInt else NODATA))
})
