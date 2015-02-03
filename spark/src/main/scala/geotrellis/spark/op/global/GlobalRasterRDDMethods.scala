package geotrellis.spark.op.global

import geotrellis.spark._

import geotrellis.raster._

import geotrellis.vector.Line

import org.apache.spark.rdd.RDD

import annotation.tailrec

trait GlobalRasterRDDMethods[K] extends RasterRDDMethods[K] {

  implicit val _sc: SpatialComponent[K]

  def verticalFlip: RasterRDD[K] = VerticalFlip(rasterRDD)

  def costDistance(points: Seq[(Int, Int)])(implicit dummy: DI): RasterRDD[K] =
    costDistance(points.map { case(c, r) => (c.toLong, r.toLong) })

  def costDistance(points: Seq[(Long, Long)]): RasterRDD[K] =
    CostDistance(rasterRDD, points)

  def costDistanceWithPath(start: (Int, Int), dest: (Int, Int))
    (implicit dummy: DI): Seq[Line] =
    costDistanceWithPath(
      (start._1.toLong, start._2.toLong),
      (dest._1.toLong, dest._2.toLong)
    )

  def costDistanceWithPath(start: (Long, Long), dest: (Long, Long)): Seq[Line] =
    CostDistance(rasterRDD, start, dest)

}
