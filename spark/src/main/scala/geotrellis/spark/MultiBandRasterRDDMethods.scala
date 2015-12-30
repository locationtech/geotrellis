package geotrellis.spark

import scala.reflect.ClassTag

trait MultiBandRasterRDDMethods[K] {
  implicit val keyClassTag: ClassTag[K]

  val rdd: MultiBandRasterRDD[K]
}
