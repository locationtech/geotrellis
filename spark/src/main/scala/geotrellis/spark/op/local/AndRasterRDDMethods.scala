package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.And
import geotrellis.spark.rdd.RasterRDD

trait AndRasterRDDMethods extends RasterRDDMethods {
  /** And a constant Int value to each cell. */
  def localAnd(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, And(r, i))
  }
  /** And a constant Int value to each cell. */
  def &(i: Int): RasterRDD = localAnd(i)
  /** And a constant Int value to each cell. */
  def &: (i: Int): RasterRDD = localAnd(i)
  /** And the values of each cell in each raster.  */
  def localAnd(other: RasterRDD): RasterRDD =
    rasterRDD.combineTiles(other) {
      case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, And(r1, r2))
    }
  /** And the values of each cell in each raster. */
  def &(rs: RasterRDD): RasterRDD = localAnd(rs)
  /** And the values of each cell in each raster.  */
  def localAnd(others: Traversable[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, And(tmsTiles.map(_.tile)))
    }
  /** And the values of each cell in each raster. */
  def &(others: Traversable[RasterRDD]): RasterRDD = localAnd(others)
}
