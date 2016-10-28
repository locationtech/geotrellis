package geotrellis.spark.mapalgebra.local

import scala.reflect.ClassTag
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd.RDD

/** Trait that defines a Methods class as an RDD[(K, Tile)] MethodsExtensions
  * having a ClassTag for K. This trait is used in the local mapalgebra case
  * because we have traits that stack onto the eventual implicit Methods class
  * for local operations. This breaks from the usual pattern of
  * marking a Methods trait as an abstract class if it needs context bounds like
  * ClassTag.
  */
private[local] trait TileRDDMethods[K] extends MethodExtensions[RDD[(K, Tile)]] {
  implicit val keyClassTag: ClassTag[K]
}
