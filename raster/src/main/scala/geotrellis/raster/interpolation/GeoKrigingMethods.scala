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

package geotrellis.raster.interpolation

import geotrellis.raster.Tile
import geotrellis.vector.{PointFeature, Extent}
import geotrellis.vector.interpolation.{GeoKriging, ModelType}

/** Methods implicitly added to tile via the package import.
  * Contains a method for each overloaded way to create a GeoKriging
  */
trait GeoKrigingMethods {
  val tile: Tile

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]]) =
    Interpolation.kriging(tile, extent)(GeoKriging(points))

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]], bandwidth: Double) =
    Interpolation.kriging(tile, extent)(GeoKriging(points, bandwidth))

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]], model: ModelType) =
    Interpolation.kriging(tile, extent)(GeoKriging(points, model))

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]], bandwidth: Double, model: ModelType) =
    Interpolation.kriging(tile, extent)(GeoKriging(points, bandwidth, model))

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]], attrFunc: (Double, Double) => Array[Double]) =
    Interpolation.kriging(tile, extent)(GeoKriging(points, attrFunc))

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]], attrFunc: (Double, Double) => Array[Double], bandwidth: Double) =
    Interpolation.kriging(tile, extent)(GeoKriging(points, attrFunc, bandwidth))

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]], attrFunc: (Double, Double) => Array[Double], model: ModelType) =
    Interpolation.kriging(tile, extent)(GeoKriging(points, attrFunc, model))

  def geoKriging(extent: Extent, points: Array[PointFeature[Double]], attrFunc: (Double, Double) => Array[Double], bandwidth: Double, model: ModelType) =
    Interpolation.kriging(tile, extent)(GeoKriging(points, attrFunc, bandwidth, model))
}
