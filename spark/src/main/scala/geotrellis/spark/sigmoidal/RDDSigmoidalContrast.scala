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

package geotrellis.spark.sigmoidal

import geotrellis.raster._
import geotrellis.raster.sigmoidal.SigmoidalContrast
import geotrellis.spark._

import org.apache.spark.rdd.RDD


object RDDSigmoidalContrast {

  /**
    * Given an RDD of Tile objects and parameters alpha and beta,
    * return an RDD of tiles upon-which the sigmoidal contrast
    * operation has been performed.
    *
    * The approach used is described here:
    * https://www.imagemagick.org/Usage/color_mods/#sigmoidal
    *
    * @param  rdd    An RDD of input tiles
    * @param  alpha  The center around-which the stretch is performed (given as a fraction)
    * @param  beta   The standard deviation in the computation, used to avoid saturating the upper and lower parts of the gamut
    * @return        An RDD of output tiles
    */
  def singleband[K, V: (? => Tile)](
    rdd: RDD[(K, V)],
    alpha: Double, beta: Double
  ): RDD[(K, Tile)] =
    rdd.map({ case (key, tile: Tile) => (key, SigmoidalContrast(tile, alpha, beta)) })

  /**
    * Given an RDD of MultibandTile objects and parameters alpha and
    * beta, return an RDD of tiles where the sigmoidal contrast
    * operation has been performed on each band of each tile.
    *
    * The approach used is described here:
    * https://www.imagemagick.org/Usage/color_mods/#sigmoidal
    *
    * @param  rdd    An RDD of input tiles
    * @param  alpha  The center around-which the stretch is performed (given as a fraction)
    * @param  beta   The standard deviation in the computation, used to avoid saturating the upper and lower parts of the gamut
    * @return        An RDD of output tiles
    */
  def multiband[K, V: (? => MultibandTile)](
    rdd: RDD[(K, V)],
    alpha: Double, beta: Double
  ): RDD[(K, MultibandTile)] =
      rdd.map({ case (key, tile: MultibandTile) => (key, SigmoidalContrast(tile, alpha, beta)) })

}
