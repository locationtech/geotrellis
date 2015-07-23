package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Pow
import geotrellis.raster.Tile

trait PowRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(r, i))
  }
  /** Pow each value of the raster by a constant value.*/
  def **(i:Int): RasterRDD[K, Tile] = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i: Int): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(i, r))
  }
  /** Pow a constant value by each cell value.*/
  def **:(i:Int): RasterRDD[K, Tile] = localPowValue(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(r, d))
  }
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double): RasterRDD[K, Tile] = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d: Double): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(d, r))
  }
  /** Pow a double constant value by each cell value.*/
  def **:(d: Double): RasterRDD[K, Tile] = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = rasterRDD.combineTiles(other) {
    case (t1, t2) => Pow(t1, t2)
  }
  /** Pow the values of each cell in each raster. */
  def **(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = localPow(other)
  /** Pow the values of each cell in each raster. */
  def localPow(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others.toSeq) {
      case tiles =>
        (tiles.head.id, Pow(tiles.map(_.tile)))
    }
  /** Pow the values of each cell in each raster. */
  def **(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] = localPow(others)
}
