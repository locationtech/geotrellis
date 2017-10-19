/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.clip

import geotrellis.vector._

import org.apache.spark.rdd._

trait Implicits {
  implicit class withFeatureClipToGridMethods[G <: Geometry, D](val self: RDD[Feature[G, D]])
      extends FeatureClipToGridMethods[G, D]

  implicit class withGeometryClipToGridMethods[G <: Geometry](val self: RDD[G])
      extends GeometryClipToGridMethods[G]
}

object Implicits extends Implicits
