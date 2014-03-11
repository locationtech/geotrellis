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
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 - z2

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else z1 - z2
}

trait SubtractOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int): RasterSource = self.mapOp(Subtract(_, i))
  /** Subtract a constant value from each cell.*/
  def -(i:Int) = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int): RasterSource = self.mapOp(Subtract(i, _))
  /** Subtract each value of a cell from a constant value. */
  def -:(i:Int) = localSubtract(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double): RasterSource = self.mapOp(Subtract(_, d))
  /** Subtract a double constant value from each cell.*/
  def -(d:Double) = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double): RasterSource = self.mapOp(Subtract(d, _))
  /** Subtract each value of a cell from a double constant value. */
  def -:(d:Double) = localSubtractFrom(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rs:RasterSource): RasterSource = self.combineOp(rs)(Subtract(_,_))
  /** Subtract the values of each cell in each raster. */
  def -(rs:RasterSource): RasterSource = localSubtract(rs)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rss:Seq[RasterSource]): RasterSource = self.combineOp(rss)(Subtract(_))
  /** Subtract the values of each cell in each raster. */
  def -(rss:Seq[RasterSource]): RasterSource = localSubtract(rss)
}

trait SubtractMethods { self: Raster =>
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int): Raster = Subtract(self, i)
  /** Subtract a constant value from each cell.*/
  def -(i: Int): Raster = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int): Raster = Subtract(i, self)
  /** Subtract each value of a cell from a constant value. */
  def -:(i: Int): Raster = localSubtract(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double): Raster = Subtract(self, d)
  /** Subtract a double constant value from each cell.*/
  def -(d: Double): Raster = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double): Raster = Subtract(d, self)
  /** Subtract each value of a cell from a double constant value. */
  def -:(d: Double): Raster = localSubtractFrom(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(r: Raster): Raster = Subtract(self, r)
  /** Subtract the values of each cell in each raster. */
  def -(r: Raster): Raster = localSubtract(r)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rs: Seq[Raster]): Raster = Subtract(self +: rs)
  /** Subtract the values of each cell in each raster. */
  def -(rs: Seq[Raster]): Raster = localSubtract(rs)
}
