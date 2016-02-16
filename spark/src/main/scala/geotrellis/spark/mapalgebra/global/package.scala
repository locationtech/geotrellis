package geotrellis.spark.mapalgebra

import geotrellis.spark._

import reflect.ClassTag

package object global {
  implicit class GlobalRasterRDDMethodExtensions[K](val self: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends GlobalRasterRDDMethods[K] with Serializable
}
