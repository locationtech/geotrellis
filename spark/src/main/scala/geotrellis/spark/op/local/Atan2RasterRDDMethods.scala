package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Atan2
import geotrellis.spark.rdd.RasterRDD

trait Atan2RasterRDDMethods extends RasterRDDMethods {
  /**
    * Operation to get the Arc Tangent2 of values.
    *  The first raster holds the y-values, and the second
    *  holds the x values. The arctan is calculated from y/x.
    *  @info A raster RDD with double rasters is always returned.
    */
  def atan2(other: RasterRDD): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Atan2(r1, r2))
  }
}
