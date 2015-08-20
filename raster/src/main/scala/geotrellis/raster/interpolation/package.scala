/*
* Copyright (c) 2015 Azavea.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package geotrellis.raster

import geotrellis.vector._

package object interpolation {
  def arrayPointFeatureDToInterpolationExtensions[D <% Double](points: Array[PointFeature[D]]) =
    new InterpolationExtensions(points.map(_.mapData { x => x: Double }))

  def traversablePointFeatureDToInterpolationExtensions[D <% Double](points: Traversable[PointFeature[D]]) =
    new InterpolationExtensions(points.map(_.mapData { x => x: Double }).toArray)

  def traversablePointFeaturesToInterpolationExtensions(points: Traversable[PointFeature[Double]]) =
    new InterpolationExtensions(points.toArray)

  implicit class InterpolationExtensions[D](val points: Array[PointFeature[Double]])
    extends SimpleKrigingMethods
    with OrdinaryKrigingMethods
    with UniversalKrigingMethods
    with GeoKrigingMethods
}
