package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Divides values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Divide extends LocalRasterBinaryOp {
  /** Divide each value of the raster by a constant value.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(_ / c)(_ / c) } 
         .withName("Divide[ByConstantInt]")

  /** Divide each value of a raster by a double constant value.*/
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(i / c).toInt})(_ / c) }
         .withName("Divide[ByConstantDouble]")

  /** Divide a constant value by each cell value.*/
  def apply(c:Op[Int],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(c / _ )(c / _ ) }
         .withName("Divide[ConstantInt]")

  /** Divide a double constant value by each cell value.*/
  def apply(c:Op[Double],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(c / i).toInt})(c / _) }
         .withName("Divide[ConstantDouble]")

  def doRasters(r1:Raster,r2:Raster) = 
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA || b == NODATA) NODATA
      else if (b == 0) NODATA
      else a / b
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a) || java.lang.Double.isNaN(b)) Double.NaN
      else if (b == 0) Double.NaN
      else a / b
    })
}

trait DivideOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int) = self.mapOp(Divide(_, i))
  /** Divide each value of the raster by a constant value.*/
  def /(i:Int) = localDivide(i)
  /** Divide a constant value by each cell value.*/
  def localDivideValue(i:Int) = self.mapOp(Divide(i,_))
  /** Divide a constant value by each cell value.*/
  def /:(i:Int) = localDivideValue(i)
  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double) = self.mapOp(Divide(_, d))
  /** Divide each value of a raster by a double constant value.*/
  def /(d:Double) = localDivide(d)
  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d:Double) = self.mapOp(Divide(d,_))
  /** Divide a double constant value by each cell value.*/
  def /:(d:Double) = localDivideValue(d)
  /** Divide the values of each cell in each raster. */
  def localDivide(rs:RasterDataSource) = self.combineOp(rs)(Divide(_,_))
  /** Divide the values of each cell in each raster. */
  def /(rs:RasterDataSource) = localDivide(rs)
}
