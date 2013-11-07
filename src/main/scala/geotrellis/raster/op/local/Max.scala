package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Gets maximum values.
 *
 * @note          Max handles NoData values such that taking the Max
 *                between a value and NoData returns NoData.
 */
object Max extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (z1 == NODATA || z2 == NODATA) NODATA
    else math.max(z1,z2)

  def combine(z1:Double,z2:Double) =
    if (isNaN(z1) || isNaN(z2)) Double.NaN
    else math.max(z1,z2)
}

trait MaxOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Max a constant Int value to each cell. */
  def localMax(i: Int) = self.mapOp(Max(_, i))
  /** Max a constant Double value to each cell. */
  def localMax(d: Double) = self.mapOp(Max(_, d))
  /** Max the values of each cell in each raster.  */
  def localMax(rs:RasterSource) = self.combineOp(rs)(Max(_,_))
  /** Max the values of each cell in each raster.  */
  def localMax(rss:Seq[RasterSource]) = self.combineOp(rss)(Max(_))
}
