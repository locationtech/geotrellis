package geotrellis.layers.mapalgebra

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withCollectionCombineMethods[K, V](val self: Seq[(K, V)])
    extends CollectionCombineMethods[K, V]

  implicit class withCollectionCombineTraversableMethods[K, V](rs: Traversable[Seq[(K, V)]]) {
    def combineValues[R](f: Traversable[V] => R): Seq[(K, R)] =
      rs.head.combineValues(rs.tail)(f)
  }

  implicit class withCollectionMapValuesTupleMethods[K, V](val self: Seq[(K, (V, V))]) extends MethodExtensions[Seq[(K, (V, V))]] {
    def combineValues[R](f: (V, V) => R): Seq[(K, R)] =
      self.mapValues { case (v1, v2) => f(v1, v2) }
  }

  implicit class withCollectionMapValuesOptionMethods[K, V](val self: Seq[(K, (V, Option[V]))]) extends MethodExtensions[Seq[(K, (V, Option[V]))]] {
    def updateValues(f: (V, V) => V): Seq[(K, V)] =
      self.mapValues { case (v1, ov2) =>
        ov2 match {
          case Some(v2) => f(v1, v2)
          case None => v1
        }
      }
  }
}
