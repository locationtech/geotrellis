package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

/**
 * Xor's cell values of rasters or Int values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed rasters, the values
 *              will be rounded to Ints.
 */
object Xor extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) = 
    if (z1 == NODATA || z2 == NODATA) NODATA
    else z1 ^ z2

  def combine(z1:Double,z2:Double) = 
    if(isNaN(z1) || isNaN(z2)) Double.NaN
    else i2d(d2i(z1) ^ d2i(z2))
}

trait XorOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) = self.mapOp(Xor(_, i))
  /** Xor a constant Int value to each cell. */
  def ^(i:Int) = localXor(i)
  /** Xor a constant Int value to each cell. */
  def ^:(i:Int) = localXor(i)
  /** Xor the values of each cell in each raster.  */
  def localXor(rs:RasterSource) = self.combine(rs)(Xor(_,_))
  /** Xor the values of each cell in each raster. */
  def ^(rs:RasterSource) = localXor(rs)
  /** Xor the values of each cell in each raster. */
  def localXor(rss:Seq[RasterSource]) = self.combine(rss)(Xor(_))
  /** Xor the values of each cell in each raster. */
  def ^(rss:Seq[RasterSource]) = localXor(rss)
}
