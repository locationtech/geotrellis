package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Abs
import geotrellis.spark.rdd.RasterRDD

trait AbsRasterRDDMethods extends RasterRDDMethods {
  /** Takes the Absolute value of each raster cell value. */
  def abs: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Abs(r)) }
}
