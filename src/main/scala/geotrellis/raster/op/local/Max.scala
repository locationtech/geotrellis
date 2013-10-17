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
  /** Takes a Raster and an Int, and gives a raster with each cell being
   * the max value of the original raster and the integer. */
  def apply(r:Op[Raster], c:Op[Int]) = 
    (r,c).map { (r,c) => 
           if(c == NODATA) {
             r.dualMapIfSet(z=>NODATA)(z=>Double.NaN)
           } else {
             r.dualMapIfSet(math.max(_, c))(math.max(_, c))
           }
          }
         .withName("Max[ConstantInt]")

  /** Takes a Raster and an Double, and gives a raster with each cell being
   * the max value of the original raster and the Double. */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI) = 
    (r,c).map { (r,c) => 
           if(java.lang.Double.isNaN(c)) {
             r.dualMapIfSet(z=>NODATA)(z=>Double.NaN)
           } else {
             r.dualMapIfSet(math.max(_, c.toInt))(math.max(_, c))
           }
          }
         .withName("Max[ConstantDouble]")

  def doRasters(r1:Raster,r2:Raster):Raster =
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else math.max(a, b)
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a) || java.lang.Double.isNaN(b)) Double.NaN
      else math.max(a,b)
    })
}

trait MaxOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Max a constant Int value to each cell. */
  def localMax(i: Int) = self.mapOp(Max(_, i))
  /** Max a constant Double value to each cell. */
  def localMax(d: Double) = self.mapOp(Max(_, d))
  /** Max the values of each cell in each raster.  */
  def localMax(rs:RasterSource) = self.combineOp(rs)(Max(_,_))
}
