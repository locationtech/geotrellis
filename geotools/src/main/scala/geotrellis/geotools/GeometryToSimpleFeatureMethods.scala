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

package geotrellis.geotools

import geotrellis.proj4._
import geotrellis.util.MethodExtensions
import geotrellis.vector._

import org.opengis.feature.simple.SimpleFeature


trait GeometryToSimpleFeatureMethods[G <: Geometry] extends MethodExtensions[G] {

  def toSimpleFeature(): SimpleFeature =
    GeometryToSimpleFeature(self, None, Seq.empty[(String, Any)])

  def toSimpleFeature(crs: CRS): SimpleFeature =
    GeometryToSimpleFeature(self, Some(crs), Seq.empty[(String, Any)])

  def toSimpleFeature(map: Map[String, Any]): SimpleFeature =
    GeometryToSimpleFeature(self, None, map.toList)

  def toSimpleFeature(crs: CRS, map: Map[String, Any]): SimpleFeature =
    GeometryToSimpleFeature(self, Some(crs), map.toList)

  def toSimpleFeature(featureId: String): SimpleFeature =
    GeometryToSimpleFeature(self, None, Seq.empty[(String, Any)], featureId)

  def toSimpleFeature(crs: CRS, featureId: String): SimpleFeature =
    GeometryToSimpleFeature(self, Some(crs), Seq.empty[(String, Any)], featureId)

  def toSimpleFeature(map: Map[String, Any], featureId: String): SimpleFeature =
    GeometryToSimpleFeature(self, None, map.toList, featureId)

  def toSimpleFeature(crs: CRS, map: Map[String, Any], featureId: String): SimpleFeature =
    GeometryToSimpleFeature(self, Some(crs), map.toList, featureId)
}
