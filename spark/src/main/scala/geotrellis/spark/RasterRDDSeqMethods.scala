package geotrellis.spark

import geotrellis.raster._
import scala.reflect.ClassTag
import org.apache.spark.rdd.RDD

trait RasterRDDSeqMethods[K] extends MethodExtensions[Traversable[RDD[(K, Tile)]]] {
  implicit val keyClassTag: ClassTag[K]
}
