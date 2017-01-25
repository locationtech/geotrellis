/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  implicit class withCollectionCombineMethods[K, V](val self: Seq[(K, V)])
    extends CollectionCombineMethods[K, V]

  implicit class withCombineTraversableMethods[K: ClassTag, V: ClassTag](rs: Traversable[RDD[(K, V)]]) {
    def combineValues[R: ClassTag](f: Traversable[V] => R, partitioner: Option[Partitioner] = None): RDD[(K, R)] =
      rs.head.combineValues(rs.tail, partitioner)(f)
  }

  implicit class withCollectionCombineTraversableMethods[K, V](rs: Traversable[Seq[(K, V)]]) {
    def combineValues[R](f: Traversable[V] => R): Seq[(K, R)] =
      rs.head.combineValues(rs.tail)(f)
  }

  implicit class withMapValuesTupleMethods[K: ClassTag, V: ClassTag](val self: RDD[(K, (V, V))]) extends MethodExtensions[RDD[(K, (V, V))]] {
    def combineValues[R: ClassTag](f: (V, V) => R): RDD[(K, R)] =
      self.mapValues { case (v1, v2) => f(v1, v2) }
  }

  implicit class withCollectionMapValuesTupleMethods[K, V](val self: Seq[(K, (V, V))]) extends MethodExtensions[Seq[(K, (V, V))]] {
    def combineValues[R](f: (V, V) => R): Seq[(K, R)] =
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
