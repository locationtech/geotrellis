package geotrellis.spark.op

import scala.reflect.ClassTag
import geotrellis.raster.{MethodExtensions, Tile}
import org.apache.spark.rdd.RDD

trait TileRDDMethods[K] extends MethodExtensions[RDD[(K, Tile)]] {
  implicit val keyClassTag: ClassTag[K]
}
