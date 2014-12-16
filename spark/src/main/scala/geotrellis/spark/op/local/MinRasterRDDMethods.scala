package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Min

trait MinRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Min(r, i))
  }
  /** Min a constant Double value to each cell. */
  def localMin(d: Double): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Min(r, d))
  }
  /** Min the values of each cell in each raster.  */
  def localMin(other: RasterRDD[K]): RasterRDD[K] = rasterRDD.combinePairs(other) {
    case ((t1, r1), (t2, r2)) => (t1, Min(r1, r2))
  }
  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD.combineRows(others.toSeq) {
      case tiles =>
        (tiles.head.id, Min(tiles.map(_.tile)))
    }
}
