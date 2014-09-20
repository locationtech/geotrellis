package geotrellis.spark.op

import geotrellis.spark.rdd.RasterRDD

package object global {
  implicit class GlobalRasterRDDExtensions(val rasterRDD: RasterRDD)
      extends GlobalRasterRDDMethods { }
}
