package geotrellis.spark.mapalgebra

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withCombineMethods[K: ClassTag, V: ClassTag](val self: RDD[(K, V)])
    extends CombineMethods[K, V]

  implicit class withCombineTraversableMethods[K: ClassTag, V: ClassTag](rs: Traversable[RDD[(K, V)]]) {
    def combineValues[R: ClassTag](f: Traversable[V] => R, partitioner: Option[Partitioner] = None): RDD[(K, R)] =
      rs.head.combineValues(rs.tail, partitioner)(f)
  }

  implicit class withMapValuesTupleMethods[K: ClassTag, V: ClassTag](val self: RDD[(K, (V, V))]) extends MethodExtensions[RDD[(K, (V, V))]] {
    def combineValues[R: ClassTag](f: (V, V) => R): RDD[(K, R)] =
      self.mapValues { case (v1, v2) => f(v1, v2) }
  }

  implicit class withMapValuesOptionMethods[K: ClassTag, V: ClassTag](val self: RDD[(K, (V, Option[V]))]) extends MethodExtensions[RDD[(K, (V, Option[V]))]] {
    def updateValues(f: (V, V) => V): RDD[(K, V)] =
      self.mapValues { case (v1, ov2) =>
        ov2 match {
          case Some(v2) => f(v1, v2)
          case None => v1
        }
    }
  }
}
