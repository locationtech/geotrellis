package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Multiplies values.
 */
object Multiply {
  /** Multiply a constant value from each cell.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(_ * c)(_ * c) }
         .withName("Multiply[ConstantInt]")

  /** Multiply a double constant value from each cell.*/
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(i * c).toInt})(_ * c) }
         .withName("Multiply[ConstantDouble]")

  /** Multiply the values of each cell in each raster. */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI):Op[Raster] = 
    (r1,r2).map(multiplyRasters)
           .withName("Multiply[Rasters]")

  /** Multiply the values of each cell in each raster. */
  def apply(rs:Seq[Op[Raster]])(implicit d:DI):Op[Raster] = 
    rs.mapOps(_.reduce(multiplyRasters))
      .withName("Multiply[Rasters]")

  /** Multiply the values of each cell in each raster. */
  def apply(rs:Op[Seq[Op[Raster]]]):Op[Raster] = 
    rs.flatMap { seq:Seq[Op[Raster]] => apply(seq:_*) }
      .withName("Multiply[Rasters]")

  /** Multiply the values of each cell in each raster. */
  def apply(rs:Op[Raster]*):Op[Raster] = 
    apply(rs)

  def multiplyRasters(r1:Raster,r2:Raster) = 
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA) b
      else if (b == NODATA) a
      else a * b
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a)) b
      else if (java.lang.Double.isNaN(b)) a
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
