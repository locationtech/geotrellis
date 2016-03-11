package geotrellis.spark

import geotrellis.raster._
import geotrellis.util.MethodExtensions
import scala.reflect.ClassTag
import org.apache.spark.rdd.RDD


trait TileLayerRDDSeqMethods[K] extends MethodExtensions[Traversable[RDD[(K, Tile)]]] {
  implicit val keyClassTag: ClassTag[K]
}
