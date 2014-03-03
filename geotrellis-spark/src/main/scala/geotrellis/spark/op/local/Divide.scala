package geotrellis.spark.op.local

import geotrellis.raster.op.local.Divide
import geotrellis.spark.rdd.RasterRDD

trait DivideOpMethods[+Repr <: RasterRDD] extends LocalBinaryOpMethods[Repr] { self: Repr =>
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int) = 
    self.mapOp[Int](i)({ case ((t, r), i) => (t, Divide(r, i)) })
  /** Divide each value of the raster by a constant value.*/
  def /(i: Int) = localDivide(i)
  /** Divide a constant value by each cell value.*/
  def localDivideValue(i: Int) = 
    self.mapOp[Int](i)({ case ((t, r), i) => (t, Divide(i, r)) })
  /** Divide a constant value by each cell value.*/
  def /:(i: Int) = localDivideValue(i)
  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double) = 
    self.mapOp[Double](d)({ case ((t, r), d) => (t, Divide(r, d)) })
  /** Divide each value of a raster by a double constant value.*/
  def /(d: Double) = localDivide(d)
  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d: Double) = 
    self.mapOp[Double](d)({ case ((t, r), d) => (t, Divide(d, r)) })
  /** Divide a double constant value by each cell value.*/
  def /:(d: Double) = localDivideValue(d)
  /** Divide the values of each cell in each raster. */
  def localDivide(rdd: RasterRDD) = 
    self.combineOp(rdd)({ case ((t1, r1), (t2, r2)) => (t1, Divide(r1, r2)) })
  /** Divide the values of each cell in each raster. */
  def /(rdd: RasterRDD) = localDivide(rdd)
}