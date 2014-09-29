package geotrellis.spark.op

import geotrellis.spark.rdd.RasterRDD

package object focal {
  implicit class FocalRasterRDDExtensions(val rasterRDD: RasterRDD)
      extends FocalRasterRDDMethods { }
}
