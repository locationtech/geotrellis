package geotrellis.raster.rasterize

import geotrellis.vector.{Geometry, Feature}
import geotrellis.raster.RasterExtent


object Implicits extends Implicits

trait Implicits {
  implicit class withRasterExtentRasterizeMethods(val self: RasterExtent) extends RasterExtentRasterizeMethods[RasterExtent]

  implicit class withGeometryRasterizeMethods(val self : Geometry) extends GeometryRasterizeMethods

  implicit class withFeatureIntRasterizeMethods(val self : Feature[Geometry, Int]) extends FeatureIntRasterizeMethods[Geometry]

  implicit class withFeatureDoubleRasterizeMethods(val self : Feature[Geometry, Double]) extends FeatureDoubleRasterizeMethods[Geometry]
}
