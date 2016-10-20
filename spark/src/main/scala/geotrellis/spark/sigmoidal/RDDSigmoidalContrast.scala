/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.spark.sigmoidal

import geotrellis.raster._
import geotrellis.raster.sigmoidal.SigmoidalContrast
import geotrellis.spark._

import org.apache.spark.rdd.RDD

import scala.reflect._


object RDDSigmoidalContrast {

  def singleband[K, V: (? => Tile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M],
    alpha: Double, beta: Double
  ): RDD[(K, Tile)] with Metadata[M] = {
    ContextRDD(
      rdd.map({ case (key, tile: Tile) =>
        (key, SigmoidalContrast(tile, alpha, beta)) }),
      rdd.metadata
    )
  }

  def multiband[K, V: (? => MultibandTile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M],
    alpha: Double, beta: Double
  ): RDD[(K, MultibandTile)] with Metadata[M] = {
    ContextRDD(
      rdd.map({ case (key, tile: MultibandTile) =>
        (key, SigmoidalContrast(tile, alpha, beta)) }),
      rdd.metadata
    )
  }

}
