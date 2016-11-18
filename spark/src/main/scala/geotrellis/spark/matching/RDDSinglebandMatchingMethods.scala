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

package geotrellis.spark.matching

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


abstract class RDDSinglebandMatchingMethods[K, V: (? => Tile)] extends MethodExtensions[RDD[(K, V)]] {

  /**
    * Given a target histogram, this function produces an RDD of
    * key-tile pairs where the histograms of the result tiles have
    * been matched to the target histogram.
    *
    * @param  targetHistogram  The histogram that the tiles should be matched to
    * @return                  An RDD key-tile pairs where the histograms have been matched
    */
  def matchHistogram[T <: AnyVal](targetHistogram: Histogram[T]): RDD[(K, Tile)] =
    RDDHistogramMatching.singleband(self, targetHistogram)

  /**
    * Given a source histogram (ostensibly that of the tiles in the
    * RDD), and a target histogram, this function produces an RDD of
    * key-tile pairs where the histograms of the result tiles have
    * been matched to the target histogram.
    *
    * @param  sourceHistogram  The ostensible histogram of the tiles of the RDD
    * @param  targetHistogram  The histogram that the tiles should be matched to
    * @return                  An RDD key-tile pairs where the histograms have been matched
    */
  def matchHistogram[T1 <: AnyVal, T2 <: AnyVal](
    sourceHistogram: Histogram[T1],
    targetHistogram: Histogram[T2]
  ): RDD[(K, Tile)] =
    RDDHistogramMatching.singleband(self, sourceHistogram, targetHistogram)

}
