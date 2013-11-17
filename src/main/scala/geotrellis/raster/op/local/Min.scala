package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Gets minimum values.
 *
 * @note          Min handles NoData values such that taking the Min
 *                between a value and NoData returns NoData.
 */
object Min extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else math.min(z1,z2)

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else math.min(z1,z2)
}

trait MinOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Min a constant Int value to each cell. */
  def localMin(i: Int) = self.mapOp(Min(_, i))
  /** Min a constant Double value to each cell. */
  def localMin(d: Double) = self.mapOp(Min(_, d))
  /** Min the values of each cell in each raster.  */
  def localMin(rs:RasterSource) = self.combineOp(rs)(Min(_,_))
  /** Min the values of each cell in each raster.  */
  def localMin(rss:Seq[RasterSource]) = self.combineOp(rss)(Min(_))
}
