package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Defined
import geotrellis.spark.rdd.RasterRDD

trait DefinedRasterRDDMethods extends RasterRDDMethods {
  /**
    * Maps an integer typed raster RDD with tiles
    * to 1 if the cell value is not NODATA, otherwise 0.
    */
  def defined: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Defined(r)) }
}
