package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.raster.op.local.Majority
import geotrellis.spark.rdd.RasterRDD

trait MajorityRasterRDDMethods extends RasterRDDMethods {
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(others: Traversable[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Majority(tmsTiles.map(_.tile)))
    }
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(rs: RasterRDD*)(implicit d: DI): RasterRDD =
    localMajority(rs)
  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, others: Traversable[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Majority(n, tmsTiles.map(_.tile)))
    }

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, rs: RasterRDD*)(implicit d: DI): RasterRDD =
    localMajority(n, rs)
}
