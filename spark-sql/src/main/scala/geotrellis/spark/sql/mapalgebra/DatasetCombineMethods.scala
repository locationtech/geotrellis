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

package geotrellis.spark.sql.mapalgebra

import geotrellis.spark.sql.KryoEncoderImplicits
import geotrellis.util.MethodExtensions

import org.apache.spark.sql.Dataset

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

// strong restriction on K type, to have possibilty to join by key
abstract class DatasetCombineMethods[K <: Product: TypeTag: ClassTag, V: ClassTag] extends MethodExtensions[Dataset[(K, V)]] with KryoEncoderImplicits {
  val ss = self.sparkSession
  import ss.implicits._

  // Tried to use this join, but that's impossible due to wrong compared binary blobs of type K
  def combineValues[R: ClassTag](other: Dataset[(K, V)])(f: (V, V) => R): Dataset[(K, R)] =
    self.join(other, "_1").as[(K, V, V)].map { case (key, tile1, tile2) => key -> f(tile1, tile2) }

  /**
    * Instead of the function above can be used this, that may allow us to remove constrain on K
    * to be a subtype of Product
    *
    * def combineValues[R: ClassTag](other: Dataset[(K, V)])(f: (V, V) => R): Dataset[(K, R)] =
    *   self.rdd.combineValues(other.rdd)(f).toDS()
    */

  def combineValues[R: ClassTag](others: Traversable[Dataset[(K, V)]])(f: Iterable[V] => R): Dataset[(K, R)] =
    (self :: others.toList).reduce(_ union _).groupByKey({ case (k, _) => k }).mapGroups({ case (k, v) => k -> f(v.map(_._2).toIterable)})

  /**
    * def combineValues[R: ClassTag](others: Traversable[Dataset[(K, V)]])(f: Iterable[V] => R): Dataset[(K, R)] =
    * (self :: others.toList).reduce(_ union _).rdd.groupByKey().toDS().mapValues(f)
    */

  def mapValues[U: ClassTag](f: V => U): Dataset[(K, U)] = self.map { case (k, v) => k -> f(v) }
}
