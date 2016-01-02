package geotrellis.spark

import scala.reflect.ClassTag

trait RasterRDDSeqMethods[K] {
  implicit val keyClassTag: ClassTag[K]
  val rasterRDDs: Traversable[RasterRDD[K]] 
}
