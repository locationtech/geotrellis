package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

/**
 * Operation to And values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed rasters, the values
 *              will be rounded to Ints.
 */
object And extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 & z2

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else i2d(d2i(z1) & d2i(z2))
}

trait AndOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** And a constant Int value to each cell. */
  def localAnd(i: Int) = self.mapOp(And(_, i))
  /** And a constant Int value to each cell. */
  def &(i:Int) = localAnd(i)
  /** And a constant Int value to each cell. */
  def &:(i:Int) = localAnd(i)
  /** And the values of each cell in each raster.  */
  def localAnd(rs:RasterSource) = self.combineOp(rs)(And(_,_))
  /** And the values of each cell in each raster. */
  def &(rs:RasterSource) = localAnd(rs)
  /** And the values of each cell in each raster.  */
  def localAnd(rss:Seq[RasterSource]) = self.combineOp(rss)(And(_))
  /** And the values of each cell in each raster. */
  def &(rss:Seq[RasterSource]) = localAnd(rss)
}
