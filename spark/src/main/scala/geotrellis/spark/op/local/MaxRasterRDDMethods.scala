package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.raster.op.local.Max
import geotrellis.spark.rdd.RasterRDD

trait MaxRasterRDDMethods extends RasterRDDMethods {
  /** Max a constant Int value to each cell. */
  def localMax(i: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Max(r, i))
  }
  /** Max a constant Double value to each cell. */
  def localMax(d: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Max(r, d))
  }
  /** Max the values of each cell in each raster.  */
  def localMax(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Max(r1, r2))
  }
  /** Max the values of each cell in each raster.  */
  def localMax(others: Seq[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Max(tmsTiles.map(_.tile)))
    }
}
