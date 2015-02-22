package geotrellis.graph.op

import geotrellis.spark._
import geotrellis.graph._

import geotrellis.vector.Line

import reflect.ClassTag

object CostDistance {

  def apply[K](graphRDD: GraphRDD[K], points: Seq[(Long, Long)])
    (implicit keyClassTag: ClassTag[K], _sc: SpatialComponent[K]): RasterRDD[K] =
    graphRDD.shortestPath(points).toRaster

  def apply[K](graphRDD: GraphRDD[K], start: (Long, Long), dest: (Long, Long))
    (implicit keyClassTag: ClassTag[K], _sc: SpatialComponent[K]): Seq[Line] =
    graphRDD.shortestPath(start, dest)

}
