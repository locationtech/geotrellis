package geotrellis.spark.op

import geotrellis.raster._
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

abstract class CombineMethods[K: ClassTag, V: ClassTag] extends MethodExtensions[RDD[(K, V)]] {
  def combineValues[R: ClassTag](other: RDD[(K, V)])(f: (V, V) => R): RDD[(K, R)] =
    self
      .join(other)
      .map { case (key, (tile1, tile2)) => key -> f(tile1, tile2) }

  def combineValues[R: ClassTag](others: Traversable[RDD[(K, V)]])(f: Iterable[V] => R): RDD[(K, R)] =
    self
      .union(others.reduce(_ union _))
      .groupByKey()
      .map { case (key, tiles) => (key, f(tiles)) }
}