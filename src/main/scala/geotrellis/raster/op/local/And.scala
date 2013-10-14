package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

/**
 * Operation for And'ing values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 */
object And {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)(_ & _)
  /** And's an integer and raster cell values. See [[AndConstant]] */
  def apply(r:Op[Raster], c:Op[Int]) = AndConstant(r, c)
  /** And's an integer and raster cell values. See [[AndConstant]] */
  def apply(c:Op[Int], r:Op[Raster])(implicit d:DummyImplicit) = AndConstant(r, c)
  /**  And's the cell values of two rasters together. See [[AndRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = AndRaster(r1, r2)
}

/**
 * And's an integer and raster cell values
 *
 * @note               If used with Double typed rasters, the values
 *                     will be rounded to Ints before and'ing.
 */
case class AndConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => AndThen(logic.RasterMapIfSet(r)(_ & c))
})

/**
 * And's the cell values of two rasters together.
 * 
 * @note               If used with Double typed rasters, the values
 *                     will be rounded to Ints before and'ing.
 */
case class AndRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => AndThen(logic.RasterCombine(r1,r2)(_ & _))
})

trait AndOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** And a constant Int value to each cell. See [[AndConstant]] */
  def localAnd(i: Int) = self.mapOp(And(_, i))
  def ^(i:Int) = localAnd(i)
  /** And the values of each cell in each raster. See [[AndRaster]] */
  def localAnd(rs:RasterSource) = self.combineOp(rs)(And(_,_))
  def ^(rs:RasterSource) = localAnd(rs)
}
