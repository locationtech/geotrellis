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

object Implicits extends Implicits

trait Implicits {
  implicit class withDatasetCombineMethods[K <: Product: TypeTag: ClassTag, V: ClassTag](val self: Dataset[(K, V)])
    extends DatasetCombineMethods[K, V]

  implicit class withDatasetMapValuesTupleMethods[K <: Product: TypeTag: ClassTag, V: ClassTag](val self: Dataset[(K, (V, V))]) extends MethodExtensions[Dataset[(K, (V, V))]] with KryoEncoderImplicits {
    def combineValues[R: ClassTag](f: (V, V) => R): Dataset[(K, R)] =
      self.mapValues { case (v1, v2) => f(v1, v2) }
  }

  implicit class withDatasetMapValuesOptionMethods[K <: Product: TypeTag: ClassTag, V: ClassTag](val self: Dataset[(K, (V, Option[V]))]) extends MethodExtensions[Dataset[(K, (V, Option[V]))]] with KryoEncoderImplicits {
    def updateValues(f: (V, V) => V): Dataset[(K, V)] =
      self.mapValues { case (v1, ov2) =>
        ov2 match {
          case Some(v2) => f(v1, v2)
          case None => v1
        }
      }
  }
}
