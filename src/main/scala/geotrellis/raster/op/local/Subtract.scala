package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Subtracts values.
 */
object Subtract {
  /** Subtract a constant value from each cell.*/
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(_ - c)(_ - c) }
         .withName("Subtract[ConstantInt]")

  /** Subtract a double constant value from each cell.*/
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(i - c).toInt})(_ - c) }
         .withName("Subtract[ConstantDouble]")

  /** Subtract each value of a cell from a constant value. */
  def apply(c:Op[Int],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(c - _)(c - _) }
         .withName("Subtract[FromConstantInt]")

  /** Subtract each value of a cell from a double constant value. */
  def apply(c:Op[Double],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(i - c).toInt})(_ - c) }
         .withName("Subtract[FromConstantDouble]")

  /** Subtract the values of each cell in each raster. */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI,d5:DI):Op[Raster] = 
    (r1,r2).map(subtractRasters)
           .withName("Subtract[Rasters]")

  def apply(rs:Seq[Op[Raster]]):Op[Raster] = 
    rs.mapOps(_.reduce(subtractRasters))
      .withName("Subtract[Rasters]")

  def apply(rs:Array[Op[Raster]]):Op[Raster] = 
    rs.mapOps(_.reduce(subtractRasters))
      .withName("Subtract[Rasters]")

  def apply(rs:Op[Seq[Raster]]):Op[Raster] = 
    rs.map(_.reduce(subtractRasters))
      .withName("Subtract[Rasters]")

  def apply(rs:Op[Array[Raster]])(implicit d:DI):Op[Raster] = 
    rs.map(_.reduce(subtractRasters))
      .withName("Subtract[Rasters]")

  def apply(rs:Op[Seq[Op[Raster]]])(implicit d:DI,d2:DI):Op[Raster] = 
    rs.flatMap { seq:Seq[Op[Raster]] => apply(seq:_*) }
      .withName("Subtract[Rasters]")

  def apply(rs:Op[Raster]*)(implicit d:DI,d2:DI,d3:DI):Op[Raster] = 
    apply(rs)

  def subtractRasters(r1:Raster,r2:Raster) = 
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA) b
      else if (b == NODATA) a
      else a - b
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a)) b
      else if (java.lang.Double.isNaN(b)) a
      else a - b
    })
}

trait SubtractOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int) = self.mapOp(Subtract(_, i))
  /** Subtract a constant value from each cell.*/
  def -(i:Int) = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int) = self.mapOp(Subtract(i, _))
  /** Subtract each value of a cell from a constant value. */
  def -:(i:Int) = localSubtract(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double) = self.mapOp(Subtract(_, d))
  /** Subtract a double constant value from each cell.*/
  def -(d:Double) = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double) = self.mapOp(Subtract(d, _))
  /** Subtract each value of a cell from a double constant value. */
  def -:(d:Double) = localSubtract(d)
  /** Subtract the values of each cell in each raster. See [[SubtractRasters]] */
  def localSubtract(rs:RasterSource) = self.combineOp(rs)(Subtract(_,_))
  /** Subtract the values of each cell in each raster. See [[SubtractRasters]] */
  def -(rs:RasterSource) = localSubtract(rs)
}
