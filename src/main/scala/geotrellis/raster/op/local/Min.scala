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
  /** Takes a Raster and an Int, and gives a raster with each cell being
   * the min value of the original raster and the integer. */
  def apply(r:Op[Raster], c:Op[Int]) = 
    (r,c).map { (r,c) => 
           if(c == NODATA) {
             r.dualMapIfSet(z=>NODATA)(z=>Double.NaN)
           } else {
             r.dualMapIfSet(math.min(_, c))(math.min(_, c))
           }
          }
         .withName("Min[ConstantInt]")

  /** Takes a Raster and an Double, and gives a raster with each cell being
   * the min value of the original raster and the Double. */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI) = 
    (r,c).map { (r,c) => 
           if(java.lang.Double.isNaN(c)) {
             r.dualMapIfSet(z=>NODATA)(z=>Double.NaN)
           } else {
             r.dualMapIfSet(math.min(_, c.toInt))(math.min(_, c))
           }
          }
         .withName("Min[ConstantDouble]")

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else math.min(a, b)
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a) || java.lang.Double.isNaN(b)) Double.NaN
      else math.min(a,b)
    })
}

trait MinOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Min a constant Int value to each cell. */
  def localMin(i: Int) = self.mapOp(Min(_, i))
  /** Min a constant Double value to each cell. */
  def localMin(d: Double) = self.mapOp(Min(_, d))
  /** Min the values of each cell in each raster.  */
  def localMin(rs:RasterSource) = self.combineOp(rs)(Min(_,_))
}
