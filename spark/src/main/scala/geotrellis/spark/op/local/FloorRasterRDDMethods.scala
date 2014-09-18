package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Floor
import geotrellis.spark.rdd.RasterRDD

trait FloorRasterRDDMethods extends RasterRDDMethods {
  /** Takes the Flooring of each raster cell value. */
  def floor: RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Floor(r)) }
}
