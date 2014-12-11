package geotrellis.spark

import geotrellis.spark._

import reflect.ClassTag

package object graph {

  implicit class GraphRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends GraphRasterRDDMethods[K] with Serializable

}
