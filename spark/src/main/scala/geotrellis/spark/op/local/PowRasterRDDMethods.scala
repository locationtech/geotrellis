package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Pow

trait PowRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(r, i))
  }
  /** Pow each value of the raster by a constant value.*/
  def **(i:Int): RasterRDD[K] = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i: Int): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(i, r))
  }
  /** Pow a constant value by each cell value.*/
  def **:(i:Int): RasterRDD[K] = localPowValue(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(r, d))
  }
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double): RasterRDD[K] = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d: Double): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Pow(d, r))
  }
  /** Pow a double constant value by each cell value.*/
  def **:(d: Double): RasterRDD[K] = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(other: RasterRDD[K]): RasterRDD[K] = rasterRDD.combinePairs(other) {
    case ((t1, r1), (t2, r2)) => (t1, Pow(r1, r2))
  }
  /** Pow the values of each cell in each raster. */
  def **(other: RasterRDD[K]): RasterRDD[K] = localPow(other)
  /** Pow the values of each cell in each raster. */
  def localPow(others: Seq[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD.combinePairs(others.toSeq) {
      case tiles =>
        (tiles.head.id, Pow(tiles.map(_.tile)))
    }
  /** Pow the values of each cell in each raster. */
  def **(others: Seq[RasterRDD[K]]): RasterRDD[K] = localPow(others)
}
