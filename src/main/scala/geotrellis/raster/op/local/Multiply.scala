package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Multiplies values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Multiply extends LocalRasterBinaryOp {
  /** Multiply a constant value from each cell.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(_ * c)(_ * c) }
         .withName("Multiply[ConstantInt]")

  /** Multiply a double constant value from each cell.*/
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(i * c).toInt})(_ * c) }
         .withName("Multiply[ConstantDouble]")

  def doRasters(r1:Raster,r2:Raster) = 
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else a * b
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a) || java.lang.Double.isNaN(b)) Double.NaN
      else a * b
    })
}

trait MultiplyOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) = self.mapOp(Multiply(_, i))
  /** Multiply a constant value from each cell.*/
  def *(i:Int) = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i:Int) = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) = self.mapOp(Multiply(_, d))
  /** Multiply a double constant value from each cell.*/
  def *(d:Double) = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d:Double) = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rs:RasterSource) = self.combineOp(rs)(Multiply(_,_))
  /** Multiply the values of each cell in each raster. */
  def *(rs:RasterSource) = localMultiply(rs)
}
