/*
 * Copyright 2019 Azavea
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

package geotrellis.layer.mapalgebra

import geotrellis.util.MethodExtensions



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
