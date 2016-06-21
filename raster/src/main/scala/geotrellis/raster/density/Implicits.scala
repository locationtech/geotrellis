package geotrellis.raster.density

import geotrellis.vector._

object Implicits extends Implicits

trait Implicits {
  implicit class withIntKernelDensityMethods(val self: Traversable[PointFeature[Int]]) extends IntKernelDensityMethods

  implicit class withDoubleKernelDensityMethods(val self: Traversable[PointFeature[Double]]) extends DoubleKernelDensityMethods
}
