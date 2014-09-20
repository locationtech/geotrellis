package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Min
import geotrellis.spark.rdd.RasterRDD

trait MinRasterRDDMethods extends RasterRDDMethods {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Min(r, i))
  }
  /** Min a constant Double value to each cell. */
  def localMin(d: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Min(r, d))
  }
  /** Min the values of each cell in each raster.  */
  def localMin(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Min(r1, r2))
  }
  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Min(tmsTiles.map(_.tile)))
    }
}
