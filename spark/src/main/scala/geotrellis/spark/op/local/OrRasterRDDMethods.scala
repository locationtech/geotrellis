package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Or

trait OrRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Or a constant Int value to each cell. */
  def localOr(i: Int): RasterRDD[K] = rasterRDD.mapPairs {
    case (t, r) => (t, Or(r, i))
  }
  /** Or a constant Int value to each cell. */
  def |(i: Int): RasterRDD[K] = localOr(i)
  /** Or a constant Int value to each cell. */
  def |:(i: Int): RasterRDD[K] = localOr(i)
  /** Or the values of each cell in each raster.  */
  def localOr(other: RasterRDD[K]): RasterRDD[K] = rasterRDD.combinePairs(other) {
    case ((t1, r1), (t2, r2)) => (t1, Or(r1, r2))
  }
  /** Or the values of each cell in each raster. */
  def |(r: RasterRDD[K]): RasterRDD[K] = localOr(r)
  /** Or the values of each cell in each raster.  */
  def localOr(others: Seq[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD.combinePairs(others.toSeq) {
    case tiles =>
      (tiles.head.id, Or(tiles.map(_.tile)))
  }
  /** Or the values of each cell in each raster. */
  def |(others: Seq[RasterRDD[K]]): RasterRDD[K] = localOr(others)
}
