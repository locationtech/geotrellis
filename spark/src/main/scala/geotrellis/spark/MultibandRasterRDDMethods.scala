package geotrellis.spark

import scala.reflect.ClassTag

trait MultibandRasterRDDMethods[K] {
  implicit val keyClassTag: ClassTag[K]

  val rdd: MultibandRasterRDD[K]
}
