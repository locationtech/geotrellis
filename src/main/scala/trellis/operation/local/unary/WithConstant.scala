package trellis.operation

import trellis.process.{Server,Results}
import trellis.raster.IntRaster

import scala.math.{max, min, pow}

/**
  * Multiply each cell in a raster by a constant value.
  */
trait WithIntConstant extends Operation[IntRaster] {
  val r:Operation[IntRaster]
  val c:Operation[Int]

  def childOperations = List(r, c)
  def _run(server:Server, cb:Callback) = runAsync(List(r, c), server, cb)
  val nextSteps:Steps = { case Results(List(a:IntRaster, b:Int)) => step2(a, b) }
  def step2(raster:IntRaster, constant:Int) = Some(doit(raster, constant))

  def doit(raster:IntRaster, c:Int):IntRaster
}

/**
  * Multiply each cell in a raster by a constant value.
  */
trait WithDoubleConstant extends Operation[IntRaster] {
  val r:Operation[IntRaster]
  val c:Operation[Double]

  def childOperations = List(r, c)
  def _run(server:Server, cb:Callback) = runAsync(List(r, c), server, cb)
  val nextSteps:Steps = { case Results(List(a:IntRaster, b:Double)) => step2(a, b) }
  def step2(raster:IntRaster, constant:Double) = Some(doit(raster, constant))

  def doit(raster:IntRaster, c:Double):IntRaster
}

/**
 * Add a constant value to each cell.
 */
case class AddConstant(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => z + c)
}


/**
 * Subtract a constant value from each cell.
 */
case class SubtractConstant(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => z - c)
}


/**
 * Multiply each cell by a constant.
 */
case class MultiplyConstant(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => z * c)
}
case class MultiplyDoubleConstant(r:Operation[IntRaster], c:Operation[Double]) extends WithDoubleConstant {
  def doit(raster:IntRaster, c:Double) = raster.mapIfSet(z => (z * c).toInt)
}


/**
 * Divide each cell by a constant value.
 */
case class DivideConstant(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => z / c)
}
case class DivideDoubleConstant(r:Operation[IntRaster], c:Operation[Double]) extends WithDoubleConstant {
  def doit(raster:IntRaster, c:Double) = raster.mapIfSet(z => (z / c).toInt)
}


/**
 * For each cell, divide a constant value by that cell's value.
 */
case class DivideConstantBy(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => c / z)
}
case class DivideDoubleConstantBy(r:Operation[IntRaster], c:Operation[Double]) extends WithDoubleConstant {
  def doit(raster:IntRaster, c:Double) = raster.mapIfSet(z => (c / z).toInt)
}


/**
 * Bitmask each cell by a constant value.
 */
case class Bitmask(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => z & c)
}

/**
  * Set each cell to a constant number or the corresponding cell value, whichever is highest.
  */
case class MaxConstant(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => max(z, c))
}

/**
  * Set each cell to a constant or its existing value, whichever is lowest.
  */
case class MinConstant(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => min(z, c))
}

/**
 * Raise each cell to the cth power.
 */
case class PowConstant(r:Operation[IntRaster], c:Operation[Int]) extends WithIntConstant {
  def doit(raster:IntRaster, c:Int) = raster.mapIfSet(z => pow(z, c).toInt)
}
case class PowDoubleConstant(r:Operation[IntRaster], c:Operation[Double]) extends WithDoubleConstant {
  def doit(raster:IntRaster, c:Double) = raster.mapIfSet(z => pow(z, c).toInt)
}
