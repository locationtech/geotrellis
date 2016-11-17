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


abstract class RDDMultibandMatchingMethods[K, V: (? => MultibandTile)] extends MethodExtensions[RDD[(K, V)]] {

  /**
    * Given a sequence of target histograms (of the bands of the
    * tiles), this function produces an RDD of key-MultibandTile pairs
    * where the histograms of the bands of the result tiles have been
    * matched to the respective target histograms.
    *
    * @param  targetHistograms  The histograms that the bands of the the tiles should be matched to
    * @return                   An RDD key-MultibandTile pairs where the bands of the histograms have been matched
    */
  def matchHistogram[T <: AnyVal](targetHistograms: Seq[Histogram[T]]): RDD[(K, MultibandTile)] =
    RDDHistogramMatching.multiband(self, targetHistograms)

  /**
    * Given a sequence of source histograms (ostensibly those of the
    * bands of the tiles in the RDD), and a sequence of target
    * histograms (of the bands of the tiles), this function produces
    * an RDD of key-MultibandTile pairs where the histograms of the
    * bands of the result tiles have been matched to the respective
    * target histograms.
    *
    * @param  sourceHistograms  The ostensible histograms of the bands of the tiles of the RDD
    * @param  targetHistograms  The histograms that the bands of the the tiles should be matched to
    * @return                   An RDD key-MultibandTile pairs where the bands of the histograms have been matched
    */
  def matchHistogram[T1 <: AnyVal, T2 <: AnyVal](
    sourceHistograms: Seq[Histogram[T1]],
    targetHistograms: Seq[Histogram[T2]]
  ): RDD[(K, MultibandTile)] =
    RDDHistogramMatching.multiband(self, sourceHistograms, targetHistograms)

}
