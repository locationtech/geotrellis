package geotrellis.spark.op.local

import geotrellis.raster.op.local.Add
import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

trait AddOpMethods[+Repr <: RasterRDD] extends LocalBinaryOpMethods[Repr] { self: Repr =>
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int) = 
    self.mapOp[Int](i)({ case (Tile(t, r), i) => (t, Add(r, i)) })
  /** Add a constant Int value to each cell. */
  def +(i: Int) = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +:(i: Int) = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) = 
    self.mapOp[Double](d)({ case (Tile(t, r), d) => (t, Add(r, d)) })
  /** Add a constant Double value to each cell. */
  def +(d: Double) = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d: Double) = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(rdd: RasterRDD) =
    self.combineOp(rdd)({ case (Tile(t1, r1), Tile(t2, r2)) => (t1, Add(r1, r2)) })
  /** Add the values of each cell in each raster. */
  def +(rdd: RasterRDD) = localAdd(rdd)
}
