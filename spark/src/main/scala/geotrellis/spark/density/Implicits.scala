package geotrellis.spark.density

import geotrellis.vector._

import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withRDDIntKernelDensityMethods(val self: RDD[PointFeature[Int]]) extends RDDIntKernelDensityMethods

  implicit class withRDDDoubleKernelDensityMethods(val self: RDD[PointFeature[Double]]) extends RDDDoubleKernelDensityMethods
}
