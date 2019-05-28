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

package geotrellis.layers.mapalgebra

import geotrellis.util.MethodExtensions

abstract class CollectionCombineMethods[K, V] extends MethodExtensions[Seq[(K, V)]] {
  def combineValues[R](other: Seq[(K, V)])(f: (V, V) => R): Seq[(K, R)] =
    (self ++ other).groupBy(_._1).mapValues { case Seq((_, v1), (_, v2)) => f(v1, v2) }.toSeq

  def combineValues[R](others: Traversable[Seq[(K, V)]])(f: Iterable[V] => R): Seq[(K, R)] =
    (self ++ others.flatten).groupBy(_._1).mapValues(tiles => f(tiles.map(_._2))).toSeq

  def mapValues[R](f: V => R): Seq[(K, R)] = self.map { case (k, v) => (k, f(v)) }
}
