package geotrellis

import geotrellis.spark._

import reflect.ClassTag

package object graph {

  implicit class GraphRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends GraphRasterRDDMethods[K] with Serializable

  implicit class GraphMethodExtensions[K](val graphRDD: GraphRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends GraphRDDMethods[K] with Serializable

  implicit def graphRDDToGraph[K](graphRDD: GraphRDD[K]) = graphRDD.graph

  implicit def graphRDDToGraphOps[K](graphRDD: GraphRDD[K]) = graphRDD.graph.ops

}
