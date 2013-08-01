package geotrellis.raster

import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.raster.op.focal._

/** An operation that transforms a raster.
  *
  * This class provides raster operations as methods.
  */
case class RasterOperation(val op:Op[Raster]) extends OperationWrapper(op) with HasRasterOperations {}

/** Provides method access to raster operations.
  */
trait HasRasterOperations extends OperationWrapper[Raster] {
  val op:Op[Raster]

  /**
   * Add a constant Int value to each cell. 
   * @see [[geotrellis.raster.op.local.AddConstant]]
   */
  def localAdd(c:Op[Int]) = Add(op,c)
  def +(c:Op[Int]) = Add(op,c)

  /** Add a constant Double value to each cell. 
    * @see [[geotrellis.raster.op.local.AddDoubleConstant]]
    */
  def localAdd(c:Op[Double]) = Add(op,c)
  def +(c:Op[Double]) = Add(op,c)

  def localSubtract(c:Op[Int]) = Subtract(op,c)
  def -(c:Op[Int]) = Subtract(op, c)

  def localAdd(r:Op[Raster]) = AddRasters(op, r)
  def +(r:Op[Raster]) = AddRasters(op, r)

  def localSubtract(r:Op[Raster]) = Subtract(op, r)
  def -(r:Op[Raster]) = Subtract(op, r)

  def localMultiply(c:Op[Int]) = Multiply(op,c)
  def localMultiply(r:Op[Raster]) = Multiply(op, r)

}
