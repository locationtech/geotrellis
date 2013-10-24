package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._
import geotrellis.source._

/**
 * Or's cell values of rasters or Int values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed rasters, the values
 *              will be rounded to Ints.
 */
object Or extends LocalRasterBinaryOp {
  /** Or's an integer value with each cell of the raster.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.map { z => if(z != NODATA) z | c else z } }
         .withName("Or[Constant]")

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.combine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else a | b
    })
}

trait OrOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  /** Or a constant Int value to each cell. */
  def localOr(i: Int) = self.mapOp(Or(_, i))
  /** Or a constant Int value to each cell. */
  def |(i:Int) = localOr(i)
  /** Or a constant Int value to each cell. */
  def |:(i:Int) = localOr(i)
  /** Or the values of each cell in each raster.  */
  def localOr(rs:RasterDataSource) = self.combineOp(rs)(Or(_,_))
  /** Or the values of each cell in each raster. */
  def |(rs:RasterDataSource) = localOr(rs)
}
