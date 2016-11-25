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

package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.interpolation.{ModelType, UniversalKriging}
import geotrellis.util.MethodExtensions

/** Methods implicitly added to tile via the package import.
  * Contains a method for each overloaded way to create a UniversalKriging
  */
trait UniversalKrigingMethods extends MethodExtensions[Traversable[PointFeature[Double]]] {
  def universalKriging(rasterExtent: RasterExtent) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray))

  def universalKriging(rasterExtent: RasterExtent, bandwidth: Double) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray, bandwidth))

  def universalKriging(rasterExtent: RasterExtent, model: ModelType) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray, model))

  def universalKriging(rasterExtent: RasterExtent, bandwidth: Double, model: ModelType) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray, bandwidth, model))

  def universalKriging(rasterExtent: RasterExtent, attrFunc: (Double, Double) => Array[Double]) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray, attrFunc))

  def universalKriging(rasterExtent: RasterExtent, attrFunc: (Double, Double) => Array[Double], bandwidth: Double) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray, attrFunc, bandwidth))

  def universalKriging(rasterExtent: RasterExtent, attrFunc: (Double, Double) => Array[Double], model: ModelType) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray, attrFunc, model))

  def universalKriging(rasterExtent: RasterExtent, attrFunc: (Double, Double) => Array[Double], bandwidth: Double, model: ModelType) =
    Interpolation.kriging(rasterExtent)(UniversalKriging(self.toArray, attrFunc, bandwidth, model))
}
