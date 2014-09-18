package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Cosh
import geotrellis.spark.rdd.RasterRDD

trait CoshRasterRDDMethods extends RasterRDDMethods {
  /**
    * Operation to get the hyperbolic cosine of values.
    * @info A raster RDD with double rasters is always returned.
    */
  def cosh: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Cosh(r)) }
}
