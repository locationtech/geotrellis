package geotrellis.spark.op

import geotrellis.spark._

import reflect.ClassTag

package object focal {
  implicit class FocalRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends FocalRasterRDDMethods[K] with Serializable
}
