package geotrellis.spark.op.global

import geotrellis.spark._
import geotrellis.spark.graphx._

import reflect.ClassTag

object CostDistance {

  def apply[K](rasterRDD: RasterRDD[K], points: Seq[(Int, Int)])
    (implicit keyClassTag: ClassTag[K], _sc: SpatialComponent[K]): RasterRDD[K] =
    rasterRDD.toGraph.shortestPath(points).toRaster

}
