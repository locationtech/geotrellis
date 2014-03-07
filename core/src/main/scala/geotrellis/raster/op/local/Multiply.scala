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
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 * z2

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
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
  def localMultiply(rs:RasterSource) = self.combineOp(rs)(Multiply(_,_))
  /** Multiply the values of each cell in each raster. */
  def *(rs:RasterSource) = localMultiply(rs)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rss:Seq[RasterSource]) = self.combineOp(rss)(Multiply(_))
  /** Multiply the values of each cell in each raster. */
  def *(rss:Seq[RasterSource]) = localMultiply(rss)
}

trait MultiplyMethods { self: Raster =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) = Multiply(self, i)
  /** Multiply a constant value from each cell.*/
  def *(i: Int) = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i: Int) = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) = Multiply(self, d)
  /** Multiply a double constant value from each cell.*/
  def *(d: Double) = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d: Double) = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(r: Raster) = Multiply(self,r)
  /** Multiply the values of each cell in each raster. */
  def *(r: Raster) = localMultiply(r)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rs: Seq[Raster]) = Multiply(self +: rs)
  /** Multiply the values of each cell in each raster. */
  def *(rs: Seq[Raster]) = localMultiply(rs)
}
