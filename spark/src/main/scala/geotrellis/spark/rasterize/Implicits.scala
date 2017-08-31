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

package geotrellis.spark.rasterize

import geotrellis.vector.{Geometry, Feature}
import geotrellis.raster.rasterize.CellValue
import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withGeometryRDDRasterizeMethods[G <: Geometry](val self: RDD[G])
      extends GeometryRDDRasterizeMethods[G]

  implicit class withFeatureRDDRasterizeMethods[G <: Geometry](val self: RDD[Feature[G, Double]])
      extends FeatureRDDRasterizeMethods[G]

  implicit class withCellValueFeatureRDDRasterizeMethods[G <: Geometry](val self: RDD[Feature[G, CellValue]])
      extends CellValueFeatureRDDRasterizeMethods[G]
}
