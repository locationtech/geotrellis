package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Or
import geotrellis.spark.rdd.RasterRDD

trait OrRasterRDDMethods extends RasterRDDMethods {
  /** Or a constant Int value to each cell. */
  def localOr(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Or(r, i))
  }
  /** Or a constant Int value to each cell. */
  def |(i: Int): RasterRDD = localOr(i)
  /** Or a constant Int value to each cell. */
  def |:(i: Int): RasterRDD = localOr(i)
  /** Or the values of each cell in each raster.  */
  def localOr(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Or(r1, r2))
  }
  /** Or the values of each cell in each raster. */
  def |(r: RasterRDD): RasterRDD = localOr(r)
  /** Or the values of each cell in each raster.  */
  def localOr(others: Seq[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
    case tmsTiles: Seq[TmsTile] =>
      TmsTile(tmsTiles.head.id, Or(tmsTiles.map(_.tile)))
  }
  /** Or the values of each cell in each raster. */
  def |(others: Seq[RasterRDD]): RasterRDD = localOr(others)
}
