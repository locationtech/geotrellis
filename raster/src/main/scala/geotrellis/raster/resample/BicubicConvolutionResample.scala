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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.apache.commons.math3.analysis.interpolation._

/**
  * Uses the following implementation:
  * http://www.paulinternet.nl/?page=bicubic
  */
class BicubicConvolutionResample(tile: Tile, extent: Extent)
    extends BicubicResample(tile, extent, 4) {

  private val resampler = new CubicConvolutionResample

  override def uniCubicResample(p: Array[Double], x: Double) =
    resampler.resample(p, x)
}

class CubicConvolutionResample {

  def resample(p: Array[Double], x: Double) =
    p(1) + 0.5 * x * (p(2) - p(0) + x * (2.0 * p(0) - 5.0 * p(1) + 4.0 * p(2) - p(3) + x *
      (3.0 * (p(1) - p(2)) + p(3) - p(0))))

}
