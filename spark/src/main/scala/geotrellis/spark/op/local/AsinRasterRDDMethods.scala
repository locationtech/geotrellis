package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Asin
import geotrellis.spark.rdd.RasterRDD

trait AsinRasterRDDMethods extends RasterRDDMethods {
  /**
    * Operation to get the arc sine of values.
    * Always return a rasterRDD with double rasters.
    * if abs(cell_value) > 1, return NaN in that cell.
    */
  def asin: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Asin(r)) }
}
