package geotrellis.spark.op.local

import geotrellis.raster.op.local.Multiply
import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

trait MultiplyOpMethods[+Repr <: RasterRDD] extends LocalBinaryOpMethods[Repr] { self: Repr =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) = 
    self.mapOp[Int](i)({ case (Tile(t, r), i) => (t, Multiply(r, i)) })
  /** Multiply a constant value from each cell.*/
  def *(i:Int) = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i:Int) = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) = 
    self.mapOp[Double](d)({ case (Tile(t, r), d) => (t, Multiply(r, d)) })
  /** Multiply a double constant value from each cell.*/
  def *(d:Double) = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d:Double) = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rdd: RasterRDD) = 
    self.combineOp(rdd)({ case (Tile(t1, r1), Tile(t2, r2)) => (t1, Multiply(r1, r2)) })
  /** Multiply the values of each cell in each raster. */
  def *(rdd: RasterRDD) = localMultiply(rdd)
}
