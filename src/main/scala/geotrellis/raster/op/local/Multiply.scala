package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Multiplies values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Multiply extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (z1 == NODATA || z2 == NODATA) NODATA
    else z1 * z2

  def combine(z1:Double,z2:Double) =
    if (isNaN(z1) || isNaN(z2)) Double.NaN
    else z1 * z2
}

trait MultiplyOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) = self.mapOp(Multiply(_, i))
  /** Multiply a constant value from each cell.*/
  def *(i:Int) = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i:Int) = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) = self.mapOp(Multiply(_, d))
  /** Multiply a double constant value from each cell.*/
  def *(d:Double) = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d:Double) = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rs:RasterSource) = self.combine(rs)(Multiply(_,_))
  /** Multiply the values of each cell in each raster. */
  def *(rs:RasterSource) = localMultiply(rs)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rss:Seq[RasterSource]) = self.combine(rss)(Multiply(_))
  /** Multiply the values of each cell in each raster. */
  def *(rss:Seq[RasterSource]) = localMultiply(rss)
}
