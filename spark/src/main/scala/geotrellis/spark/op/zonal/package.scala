package geotrellis.spark.op

import geotrellis.spark.rdd.RasterRDD

package object zonal {
  implicit class ZonalRasterRDDMethodExtensions(val rasterRDD: RasterRDD)
      extends ZonalRasterRDDMethods { }
}
