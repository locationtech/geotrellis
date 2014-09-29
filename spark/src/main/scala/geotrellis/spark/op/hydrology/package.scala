package geotrellis.spark.op

import geotrellis.spark.rdd.RasterRDD

package object hydrology {
  implicit class HydrologyRasterRDDExtensions(val rasterRDD: RasterRDD)
      extends HydrologyRasterRDDMethods
}
