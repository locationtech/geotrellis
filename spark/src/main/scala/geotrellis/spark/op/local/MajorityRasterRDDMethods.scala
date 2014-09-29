package geotrellis.spark.op.local

import scala.collection.immutable.Map

import geotrellis.raster._
import geotrellis.raster.op.local.Majority

import geotrellis.spark._
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
  def localMajority(rs: RasterRDD*): RasterRDD =
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
  def localMajority(n: Int, rs: RasterRDD*): RasterRDD =
    localMajority(n, rs)
}
