package geotrellis.spark

import geotrellis.spark._

import reflect.ClassTag

package object graph {

  implicit class GraphRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends GraphRasterRDDMethods[K] with Serializable

  implicit class RasterRDDGraphMethodExtensions[K, N](val graph: Graph[(K, N), Int])
    (implict val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends RasterRDDGraphMethods[K, N] with Serializable

}
