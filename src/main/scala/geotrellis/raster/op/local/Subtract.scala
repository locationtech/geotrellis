package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

/**
 * Subtracts values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Subtract extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (z1 == NODATA || z2 == NODATA) NODATA
    else z1 - z2

  def combine(z1:Double,z2:Double) =
    if (isNaN(z1) || isNaN(z2)) Double.NaN
    else z1 - z2
}

trait SubtractOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int) = self.mapOp(Subtract(_, i))
  /** Subtract a constant value from each cell.*/
  def -(i:Int) = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int) = self.mapOp(Subtract(i, _))
  /** Subtract each value of a cell from a constant value. */
  def -:(i:Int) = localSubtract(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double) = self.mapOp(Subtract(_, d))
  /** Subtract a double constant value from each cell.*/
  def -(d:Double) = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double) = self.mapOp(Subtract(d, _))
  /** Subtract each value of a cell from a double constant value. */
  def -:(d:Double) = localSubtract(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rs:RasterSource) = self.combine(rs)(Subtract(_,_))
  /** Subtract the values of each cell in each raster. */
  def -(rs:RasterSource) = localSubtract(rs)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rss:Seq[RasterSource]) = self.combine(rss)(Subtract(_))
  /** Subtract the values of each cell in each raster. */
  def -(rss:Seq[RasterSource]) = localSubtract(rss)
}
