package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Cos
import geotrellis.spark.rdd.RasterRDD

trait CosRasterRDDMethods extends RasterRDDMethods {
  /**
    * Operation to get the Cosine of values.
    * @info Always returns raster RDD with double or float rasters.
    */
  def cos: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Cos(r)) }
}
