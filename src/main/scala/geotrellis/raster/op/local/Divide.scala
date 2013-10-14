package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Divides values.
 */
object Divide {
  /** Divide each value of the raster by a constant value.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(_ / c)(_ / c) } 
         .withName("Divide[ByConstantInt]")

  /** Divide each value of a raster by a double constant value.*/
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI,d2:DI):Op[Raster] = 
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

  /** Divide the values of each cell in each raster. */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI,d5:DI):Op[Raster] = 
    (r1,r2).map(divideRasters)
           .withName("Divide[Rasters]")

  /** Divide the values of each cell in each raster. */
  def apply(rs:Seq[Op[Raster]]):Op[Raster] = 
    rs.mapOps(_.reduce(divideRasters))
      .withName("Divide[Rasters]")

  /** Divide the values of each cell in each raster. */
  def apply(rs:Array[Op[Raster]]):Op[Raster] = 
    rs.mapOps(_.reduce(divideRasters))
      .withName("Divide[Rasters]")

  /** Divide the values of each cell in each raster. */
  def apply(rs:Op[Seq[Raster]]):Op[Raster] = 
    rs.map(_.reduce(divideRasters))
      .withName("Divide[Rasters]")

  /** Divide the values of each cell in each raster. */
  def apply(rs:Op[Array[Raster]])(implicit d:DI):Op[Raster] = 
    rs.map(_.reduce(divideRasters))
      .withName("Divide[Rasters]")

  /** Divide the values of each cell in each raster. */
  def apply(rs:Op[Seq[Op[Raster]]])(implicit d:DI,d2:DI):Op[Raster] = 
    rs.flatMap { seq:Seq[Op[Raster]] => apply(seq:_*) }
      .withName("Divide[Rasters]")

  /** Divide the values of each cell in each raster. */
  def apply(rs:Op[Raster]*)(implicit d:DI,d2:DI,d3:DI):Op[Raster] = 
    apply(rs)

  def divideRasters(r1:Raster,r2:Raster) = 
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA) b
      else if (b == NODATA) a
      else if (b == 0) NODATA
      else a / b
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a)) b
      else if (java.lang.Double.isNaN(b)) a
      else if (b == 0) Double.NaN
      else a / b
    })
}

trait DivideOpMethods[+Repr <: RasterSource] { self: Repr =>
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
  def localDivide(rs:RasterSource) = self.combineOp(rs)(Divide(_,_))
  /** Divide the values of each cell in each raster. */
  def /(rs:RasterSource) = localDivide(rs)
}
