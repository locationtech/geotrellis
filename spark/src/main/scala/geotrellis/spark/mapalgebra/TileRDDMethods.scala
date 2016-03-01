package geotrellis.spark.mapalgebra

import scala.reflect.ClassTag
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd.RDD


trait TileRDDMethods[K] extends MethodExtensions[RDD[(K, Tile)]] {
  implicit val keyClassTag: ClassTag[K]
}
