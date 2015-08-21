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

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.interpolation.{Semivariogram, OrdinaryKriging}

/** Methods implicitly added to tile via the package import.
  * Contains a method for each overloaded way to create a OrdinaryKriging
  */
trait OrdinaryKrigingMethods {
  val points: Array[PointFeature[Double]]

  def ordinaryKriging(rasterExtent: RasterExtent) =
    Interpolation.kriging(rasterExtent)(OrdinaryKriging(points))

  def ordinaryKriging(rasterExtent: RasterExtent, bandwidth: Double) =
    Interpolation.kriging(rasterExtent)(OrdinaryKriging(points, bandwidth))

  def ordinaryKriging(rasterExtent: RasterExtent, sv: Semivariogram) =
    Interpolation.kriging(rasterExtent)(OrdinaryKriging(points, sv))

  def ordinaryKriging(rasterExtent: RasterExtent, bandwidth: Double, sv: Semivariogram) =
    Interpolation.kriging(rasterExtent)(OrdinaryKriging(points, bandwidth, sv))
}
