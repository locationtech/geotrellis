package geotrellis.spark.op.zonal

import geotrellis.spark.rdd.RasterRDD

package object summary {
  implicit class ZonalSummaryRasterRDDMethodExtensions(val rasterRDD: RasterRDD)
      extends ZonalSummaryRasterRDDMethods { }
}
