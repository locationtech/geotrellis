package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Ceil
import geotrellis.spark.rdd.RasterRDD

trait CeilRasterRDDMethods extends RasterRDDMethods {
  /** Takes the Ceiling of each raster cell value. */
  def ceil: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Ceil(r)) }
}
