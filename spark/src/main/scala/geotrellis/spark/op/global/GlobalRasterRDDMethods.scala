package geotrellis.spark.op.global

import geotrellis.spark._
import geotrellis.raster._
import org.apache.spark.Partitioner
import scala.reflect.ClassTag

abstract class GlobalRasterRDDMethods[K: ClassTag] extends MethodExtensions[RasterRDD[K]] {

  implicit val _sc: SpatialComponent[K]

  def verticalFlip: RasterRDD[K] = verticalFlip(None)
  def verticalFlip(partitioner: Option[Partitioner]): RasterRDD[K] = VerticalFlip(self, partitioner)

}
