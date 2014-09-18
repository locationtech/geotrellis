package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Atan
import geotrellis.spark.rdd.RasterRDD

trait AtanRasterRDDMethods extends RasterRDDMethods {
  /**
    * Operation to get the Arc Tangent of values.
    * @info A raster RDD with double rasters is always returned.
    */
  def atan: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Atan(r)) }
}
