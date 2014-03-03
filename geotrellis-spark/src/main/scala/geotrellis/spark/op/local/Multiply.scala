package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Multiply
import geotrellis.spark.rdd.RasterRDD

trait MultiplyOpMethods[+Repr <: RasterRDD] { self: Repr =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Multiply(r, i)) }
  /** Multiply a constant value from each cell.*/
  def *(i:Int) = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i:Int) = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Multiply(r, d)) }
  /** Multiply a double constant value from each cell.*/
  def *(d:Double) = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d:Double) = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rdd: RasterRDD) = 
    self.combineTiles(rdd) { case (Tile(t1, r1), Tile(t2, r2)) => Tile(t1, Multiply(r1, r2)) }
  /** Multiply the values of each cell in each raster. */
  def *(rdd: RasterRDD) = localMultiply(rdd)
}
