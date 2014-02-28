package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD
import geotrellis.raster.op.local.Subtract

trait SubtractOpMethods[+Repr <: RasterRDD] extends LocalBinaryOpMethods[Repr] { self: Repr =>
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int) = 
    self.mapOp[Int](i)({ case (Tile(t, r), i) => Tile(t, Subtract(r, i)) })
  /** Subtract a constant value from each cell.*/
  def -(i:Int) = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int) = 
    self.mapOp[Int](i)({ case (Tile(t, r), i) => Tile(t, Subtract(i, r)) })
  /** Subtract each value of a cell from a constant value. */
  def -:(i:Int) = localSubtractFrom(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double) = 
    self.mapOp[Double](d)({ case (Tile(t, r), d) => Tile(t, Subtract(r, d)) })
  /** Subtract a double constant value from each cell.*/
  def -(d:Double) = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double) = 
    self.mapOp[Double](d)({ case (Tile(t, r), d) => Tile(t, Subtract(d, r)) })
  /** Subtract each value of a cell from a double constant value. */
  def -:(d:Double) = localSubtractFrom(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rdd: RasterRDD) = 
    self.combineOp(rdd)({ case (Tile(t1, r1), Tile(t2, r2)) => Tile(t1, Subtract(r1, r2)) })
  /** Subtract the values of each cell in each raster. */
  def -(rdd: RasterRDD) = localSubtract(rdd)
  /** Subtract the values of each cell in each raster. */
}
