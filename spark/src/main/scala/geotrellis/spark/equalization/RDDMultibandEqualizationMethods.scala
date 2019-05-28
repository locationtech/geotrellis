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

package geotrellis.spark.equalization

import geotrellis.layers.Metadata
import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.spark._
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd.RDD


trait RDDMultibandEqualizationMethods[K, M] extends MethodExtensions[RDD[(K, MultibandTile)] with Metadata[M]] {

  /**
    * Equalize the histograms of the respective bands of the RDD of
    * MultibandTile objects using one joint histogram derived from
    * each band of the input RDD.
    */
  def equalize(): RDD[(K, MultibandTile)] with Metadata[M] =
    RDDHistogramEqualization.multiband(self)

  /**
    * Given a sequence of Histogram objects (one per band), equalize
    * the histograms of the respective bands of the RDD of
    * MultibandTile objects.
    *
    * @param  histograms  A sequence of histograms
    */
  def equalize[T <: AnyVal](histograms: Array[Histogram[T]]): RDD[(K, MultibandTile)] with Metadata[M] =
    RDDHistogramEqualization.multiband(self, histograms)
}
