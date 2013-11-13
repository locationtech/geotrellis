package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Operation to add values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Add extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) = 
    if (z1 == NODATA || z2 == NODATA) NODATA
    else z1 + z2

  def combine(z1:Double,z2:Double) = 
    if (isNaN(z1) || isNaN(z2)) Double.NaN
    else z1 + z2
}

trait AddOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int) = self.mapOp(Add(_, i))
  /** Add a constant Int value to each cell. */
  def +(i:Int) = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +:(i:Int) = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) = self.mapOp(Add(_, d))
  /** Add a constant Double value to each cell. */
  def +(d:Double) = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d:Double) = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(rs:RasterSource) = self.combineOp(rs)(Add(_,_))
  /** Add the values of each cell in each raster. */
  def +(rs:RasterSource) = localAdd(rs)
  /** Add the values of each cell in each raster.  */
  def localAdd(rss:Seq[RasterSource]) = self.combineOp(rss)(Add(_))
  /** Add the values of each cell in each raster. */
  def +(rss:Seq[RasterSource]) = localAdd(rss)
}
