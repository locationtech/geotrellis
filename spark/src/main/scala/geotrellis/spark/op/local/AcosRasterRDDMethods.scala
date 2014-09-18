package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Acos
import geotrellis.spark.rdd.RasterRDD

trait AcosRasterRDDMethods extends RasterRDDMethods {
  /**
    * Operation to get the arc cosine of values.
    * Always returns a raster RDD with double tiled rasters.
    * If the absolute value of the cell value is > 1, it will be NaN.
    */
  def acos: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Acos(r)) }
}
