package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.local.Max

trait MaxRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Max a constant Int value to each cell. */
  def localMax(i: Int): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Max(r, i))
  }
  /** Max a constant Double value to each cell. */
  def localMax(d: Double): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Max(r, d))
  }
  /** Max the values of each cell in each raster.  */
  def localMax(other: RasterRDD[K]): RasterRDD[K] = rasterRDD.combinePairs(other) {
    case ((t1, r1), (t2, r2)) => (t1, Max(r1, r2))
  }
  /** Max the values of each cell in each raster.  */
  def localMax(others: Seq[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD.combinePairs(others) {
      case tiles =>
        (tiles.head.id, Max(tiles.map(_.tile)))
    }
}
