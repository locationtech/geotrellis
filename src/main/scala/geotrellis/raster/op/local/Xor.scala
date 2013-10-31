package geotrellis.raster.op.local

import geotrellis._
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
  /** Xor's an integer value with each cell of the raster.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.map { z => if(z != NODATA) z ^ c else z } }
         .withName("Xor[Constant]")

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.combine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else a ^ b
    })
}

trait XorOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) = self.mapOp(Xor(_, i))
  /** Xor a constant Int value to each cell. */
  def ^(i:Int) = localXor(i)
  /** Xor a constant Int value to each cell. */
  def ^:(i:Int) = localXor(i)
  /** Xor the values of each cell in each raster.  */
  def localXor(rs:RasterSource) = self.combineOp(rs)(Xor(_,_))
  /** Xor the values of each cell in each raster. */
  def ^(rs:RasterSource) = localXor(rs)
}
