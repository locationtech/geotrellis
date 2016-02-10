package geotrellis.spark.op.local.spatial

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.raster.rasterize.Rasterize.Options
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.spark._
import geotrellis.vector._
import scala.reflect.ClassTag

abstract class LocalSpatialRasterRDDMethods[K: SpatialComponent: ClassTag] extends MethodExtensions[RasterRDD[K]] {


}
