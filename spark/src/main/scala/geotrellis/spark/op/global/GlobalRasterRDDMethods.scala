package geotrellis.spark.op.global

import geotrellis.spark._
import geotrellis.raster._
import scala.reflect.ClassTag

abstract class GlobalRasterRDDMethods[K: ClassTag] extends MethodExtensions[RasterRDD[K]] {

  implicit val _sc: SpatialComponent[K]

  def verticalFlip = VerticalFlip(self)

}
