package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Pow
import geotrellis.spark.rdd.RasterRDD

trait PowRasterRDDMethods extends RasterRDDMethods {
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Pow(r, i))
  }
  /** Pow each value of the raster by a constant value.*/
  def **(i:Int): RasterRDD = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Pow(i, r))
  }
  /** Pow a constant value by each cell value.*/
  def **:(i:Int): RasterRDD = localPowValue(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Pow(r, d))
  }
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double): RasterRDD = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Pow(d, r))
  }
  /** Pow a double constant value by each cell value.*/
  def **:(d: Double): RasterRDD = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Pow(r1, r2))
  }
  /** Pow the values of each cell in each raster. */
  def **(other: RasterRDD): RasterRDD = localPow(other)
  /** Pow the values of each cell in each raster. */
  def localPow(others: Seq[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Pow(tmsTiles.map(_.tile)))
    }
  /** Pow the values of each cell in each raster. */
  def **(others: Seq[RasterRDD]): RasterRDD = localPow(others)
}
