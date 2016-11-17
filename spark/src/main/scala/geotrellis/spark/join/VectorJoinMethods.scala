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

package geotrellis.spark.join

import geotrellis.util._
import geotrellis.vector._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext

import scala.reflect._


abstract class VectorJoinMethods[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ] extends MethodExtensions[RDD[L]] {

  def vectorJoin(other: RDD[R], pred: (Geometry, Geometry) => Boolean)(implicit sc: SparkContext) =
    VectorJoin(self, other, pred)
}
