package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.InverseMask
import geotrellis.spark.rdd.RasterRDD

trait InverseMaskRasterRDDMethods extends RasterRDDMethods {
  /**
    * Operation to get the hyperbolic cosine of values.
    * @info A raster RDD with double rasters is always returned.
    */
  def inverseMask: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Cosh(r)) }
}
