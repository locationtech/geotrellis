package geotrellis.spark.op.global

import geotrellis.spark._

import geotrellis.raster._

import geotrellis.vector.Line

import org.apache.spark.rdd.RDD

import annotation.tailrec

trait GlobalRasterRDDMethods[K] extends RasterRDDMethods[K] {

  implicit val _sc: SpatialComponent[K]

  def verticalFlip: RasterRDD[K] = VerticalFlip(rasterRDD)

}
