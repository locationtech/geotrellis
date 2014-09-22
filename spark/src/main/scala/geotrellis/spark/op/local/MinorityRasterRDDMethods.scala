package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.raster.op.local.Minority
import geotrellis.spark.rdd.RasterRDD

trait MinorityRasterRDDMethods extends RasterRDDMethods {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Seq[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Minority(tmsTiles.map(_.tile)))
    }

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: RasterRDD*)(implicit d: DI): RasterRDD =
    localMinority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Seq[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Minority(n, tmsTiles.map(_.tile)))
    }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: RasterRDD*)(implicit d: DI): RasterRDD =
    localMinority(n, rs)
}
