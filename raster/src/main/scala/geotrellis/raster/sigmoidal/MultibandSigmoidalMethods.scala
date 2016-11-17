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

package geotrellis.raster.sigmoidal

import geotrellis.raster.MultibandTile
import geotrellis.util.MethodExtensions


trait MultibandSigmoidalMethods extends MethodExtensions[MultibandTile] {

  /**
    * Given the parameters alpha and beta, perform the sigmoidal
    * contrast computation on each band and return the result as a
    * multiband tile.
    *
    * The approach used is described here:
    * https://www.imagemagick.org/Usage/color_mods/#sigmoidal
    *
    * @param  alpha  The center around-which the stretch is performed (given as a fraction)
    * @param  beta   The standard deviation in the computation, used to avoid saturating the upper and lower parts of the gamut
    * @return        The output tile
    */
  def sigmoidal(alpha: Double, beta: Double): MultibandTile = SigmoidalContrast(self, alpha, beta)

}
