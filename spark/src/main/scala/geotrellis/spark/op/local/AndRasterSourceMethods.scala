package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.And
import geotrellis.spark.rdd.RasterRDD

trait AndRasterRDDMethods extends RasterRDDMethods {
  /** And a constant Int value to each cell. */
  def localAnd(i: Int): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, And(r, i)) }
  /** And a constant Int value to each cell. */
  def &(i: Int): RasterRDD = localAnd(i)
  /** And a constant Int value to each cell. */
  def &: (i: Int): RasterRDD = localAnd(i)
  /** And the values of each cell in each raster.  */
  def localAnd(rs: RasterRDD): RasterRDD =
    rasterRDD.combineTiles(rs) {
      case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, And(r1, r2))
    }
  /** And the values of each cell in each raster. */
  def &(rs: RasterRDD): RasterRDD = localAnd(rs)
  /** And the values of each cell in each raster.  */
  /*def localAnd(rss: Seq[RasterRDD]): RasterRDD =
   rasterRDD.combineTiles(rs)(And(_))
   /** And the values of each cell in each raster. */
   def &(rss: Seq[RasterRDD]): RasterRDD = localAnd(rss)*/
}
