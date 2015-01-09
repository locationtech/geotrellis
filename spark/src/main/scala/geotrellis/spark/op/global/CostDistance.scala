package geotrellis.spark.op.global

import geotrellis.spark._
import geotrellis.spark.graph._

import geotrellis.vector.Line

import reflect.ClassTag

object CostDistance {

  def apply[K](rasterRDD: RasterRDD[K], points: Seq[(Long, Long)])
    (implicit keyClassTag: ClassTag[K], _sc: SpatialComponent[K]): RasterRDD[K] =
    rasterRDD.toGraph.shortestPath(points).toRaster

  def apply[K](rasterRDD: RasterRDD[K], start: (Long, Long), dest: (Long, Long))
    (implicit keyClassTag: ClassTag[K], _sc: SpatialComponent[K]): Set[Line] =
    rasterRDD.toGraph.shortestPath(start, dest)

}
