package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Operation to add values.
 */
object Add {
  /** Add a constant Int value to each cell. */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet(_ + c)(_ + c) }
         .withName("Add[ConstantInt]")

  /** Add a constant Double value to each cell. */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => r.dualMapIfSet({i:Int=>(i + c).toInt})(_ + c) }
         .withName("Add[ConstantDouble]")

  /** Add the values of each cell in each raster.  */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI):Op[Raster] = 
    (r1,r2).map(addRasters)
           .withName("Add[Rasters]")

  /** Add the values of each cell in each raster.  */
  def apply(rs:Seq[Op[Raster]])(implicit d:DI):Op[Raster] = 
    rs.mapOps(_.reduce(addRasters))
      .withName("Add[Rasters]")

  /** Add the values of each cell in each raster.  */
  def apply(rs:Op[Seq[Op[Raster]]]):Op[Raster] = 
    rs.flatMap { seq:Seq[Op[Raster]] => apply(seq:_*) }
      .withName("Add[Rasters]")

  /** Add the values of each cell in each raster.  */
  def apply(rs:Op[Raster]*):Op[Raster] = 
    apply(rs)

  def addRasters(r1:Raster,r2:Raster):Raster = 
    r1.dualCombine(r2)({
      (a, b) =>
      if (a == NODATA) b
      else if (b == NODATA) a
      else a + b
    })({
      (a, b) =>
      if (java.lang.Double.isNaN(a)) b
      else if (java.lang.Double.isNaN(b)) a
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
