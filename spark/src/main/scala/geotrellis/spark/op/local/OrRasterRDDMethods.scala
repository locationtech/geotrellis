package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Or
import geotrellis.raster.Tile

trait OrRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Or a constant Int value to each cell. */
  def localOr(i: Int): RasterRDD[K, Tile] = rasterRDD.mapPairs {
    case (t, r) => (t, Or(r, i))
  }
  /** Or a constant Int value to each cell. */
  def |(i: Int): RasterRDD[K, Tile] = localOr(i)
  /** Or a constant Int value to each cell. */
  def |:(i: Int): RasterRDD[K, Tile] = localOr(i)
  /** Or the values of each cell in each raster.  */
  def localOr(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = rasterRDD.combineTiles(other) {
    case (t1, t2) => Or(t1, t2)
  }
  /** Or the values of each cell in each raster. */
  def |(r: RasterRDD[K, Tile]): RasterRDD[K, Tile] = localOr(r)
  /** Or the values of each cell in each raster.  */
  def localOr(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others.toSeq) {
    case tiles =>
      (tiles.head.id, Or(tiles.map(_.tile)))
  }
  /** Or the values of each cell in each raster. */
  def |(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] = localOr(others)
}
