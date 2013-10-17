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
  /** Add a constant Int value to each cell. */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(_ + c)(_ + c) }
         .withName("Add[ConstantInt]")

  /** Add a constant Double value to each cell. */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(i + c).toInt})(_ + c) }
         .withName("Add[ConstantDouble]")

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else a + b
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a) || java.lang.Double.isNaN(b)) Double.NaN
      else a + b
    })
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
}
