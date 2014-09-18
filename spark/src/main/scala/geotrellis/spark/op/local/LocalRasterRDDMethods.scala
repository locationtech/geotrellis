package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMethods
import geotrellis.spark.rdd.RasterRDD

trait LocalRasterRDDMethods extends RasterRDDMethods
                               with AddRasterRDDMethods
                               with SubtractRasterRDDMethods
                               with MultiplyRasterRDDMethods
                               with DivideRasterRDDMethods {

  /** Takes the Absolute value of each raster cell value. */
  def localAbs(): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Abs(r)) }


}
