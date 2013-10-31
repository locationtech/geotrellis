package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Pows values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Pow extends LocalRasterBinaryOp {
  /** Raise each value of the raster to the power of a constant value.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(math.pow(_,c).toInt)(math.pow(_,c)) } 
         .withName("Pow[ByConstantInt]")

  /** Raise each value of the raster to the power of a constant double value.*/
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(math.pow(_,c).toInt)(math.pow(_,c)) }
         .withName("Pow[ByConstantDouble]")

  /** Pow a constant value by each cell value.*/
  def apply(c:Op[Int],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(math.pow(c,_).toInt)(math.pow(c,_)) }
         .withName("Pow[ConstantInt]")

  /** Pow a double constant value by each cell value.*/
  def apply(c:Op[Double],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(math.pow(c,_).toInt)(math.pow(c,_)) }
         .withName("Pow[ConstantDouble]")

  def doRasters(r1:Raster,r2:Raster) = 
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else if (b == 0) NODATA
      else math.pow(a, b).toInt
    })(
      math.pow(_,_)
    )
}

trait PowOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int) = self.mapOp(Pow(_, i))
  /** Pow each value of the raster by a constant value.*/
  def **(i:Int) = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i:Int) = self.mapOp(Pow(i,_))
  /** Pow a constant value by each cell value.*/
  def **:(i:Int) = localPow(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double) = self.mapOp(Pow(_, d))
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double) = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d:Double) = self.mapOp(Pow(d,_))
  /** Pow a double constant value by each cell value.*/
  def **:(d:Double) = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(rs:RasterSource) = self.combineOp(rs)(Pow(_,_))
  /** Pow the values of each cell in each raster. */
  def **(rs:RasterSource) = localPow(rs)
}
