package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Divide
import geotrellis.spark.rdd.RasterRDD

trait DivideOpMethods[+Repr <: RasterRDD] { self: Repr =>
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Divide(r, i)) }
  /** Divide each value of the raster by a constant value.*/
  def /(i: Int) = localDivide(i)
  /** Divide a constant value by each cell value.*/
  def localDivideValue(i: Int) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Divide(i, r)) }
  /** Divide a constant value by each cell value.*/
  def /:(i: Int) = localDivideValue(i)
  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Divide(r, d)) }
  /** Divide each value of a raster by a double constant value.*/
  def /(d: Double) = localDivide(d)
  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d: Double) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Divide(d, r)) }
  /** Divide a double constant value by each cell value.*/
  def /:(d: Double) = localDivideValue(d)
  /** Divide the values of each cell in each raster. */
  def localDivide(rdd: RasterRDD) = 
    self.combineTiles(rdd) { case (Tile(t1, r1), Tile(t2, r2)) => Tile(t1, Divide(r1, r2)) }
  /** Divide the values of each cell in each raster. */
  def /(rdd: RasterRDD) = localDivide(rdd)
}
