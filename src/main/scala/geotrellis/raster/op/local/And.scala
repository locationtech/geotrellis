package geotrellis.raster.op.local

import geotrellis._
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
  /** And a constant Int value to each cell. */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.map { z => if(z != NODATA) z & c else z } }
         .withName("And[Constant]")

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.combine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else a & b
    })
}

trait AndOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  /** And a constant Int value to each cell. */
  def localAnd(i: Int) = self.mapOp(And(_, i))
  /** And a constant Int value to each cell. */
  def &(i:Int) = localAnd(i)
  /** And a constant Int value to each cell. */
  def &:(i:Int) = localAnd(i)
  /** And the values of each cell in each raster.  */
  def localAnd(rs:RasterDataSource) = self.combineOp(rs)(And(_,_))
  /** And the values of each cell in each raster. */
  def &(rs:RasterDataSource) = localAnd(rs)
}
