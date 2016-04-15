package geotrellis.spark.mapalgebra

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


abstract class CombineMethods[K: ClassTag, V: ClassTag] extends MethodExtensions[RDD[(K, V)]] {
  def combineValues[R: ClassTag](other: RDD[(K, V)])(f: (V, V) => R): RDD[(K, R)] = combineValues(other, None)(f)
  def combineValues[R: ClassTag](other: RDD[(K, V)], partitioner: Option[Partitioner])(f: (V, V) => R): RDD[(K, R)] =
    partitioner
      .fold(self.join(other))(self.join(other, _))
      .mapValues { case (tile1, tile2) => f(tile1, tile2) }

  def combineValues[R: ClassTag](others: Traversable[RDD[(K, V)]])(f: Iterable[V] => R): RDD[(K, R)] = combineValues(others, None)(f)
  def combineValues[R: ClassTag](others: Traversable[RDD[(K, V)]], partitioner: Option[Partitioner])(f: Iterable[V] => R): RDD[(K, R)] = {
    val union = self.sparkContext.union(self :: others.toList)
    partitioner
      .fold(union.groupByKey(Partitioner.defaultPartitioner(self, others.toSeq: _*)))(union.groupByKey(_))
      .mapValues { case tiles => f(tiles) }
  }
}
