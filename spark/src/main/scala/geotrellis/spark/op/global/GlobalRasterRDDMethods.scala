package geotrellis.spark.op.global

import geotrellis.spark._

import geotrellis.raster._

import org.apache.spark.rdd.RDD

import annotation.tailrec

trait GlobalRasterRDDMethods[K] extends RasterRDDMethods[K] {

  val _sc: SpatialComponent[K]

  def verticalFlip: RasterRDD[K] = VerticalFlip(rasterRDD)

  def costDistance(points: Seq[(Int, Int)]): RasterRDD[K] =
    CostDistance(rasterRDD, points)

}
