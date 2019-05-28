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


trait RDDSinglebandEqualizationMethods[K, M] extends MethodExtensions[RDD[(K, Tile)] with Metadata[M]] {

  /**
    * Equalize the histograms of the respective Tile objects in the
    * RDD using one joint histogram derived from the entire source
    * RDD.
    */
  def equalize(): RDD[(K, Tile)] with Metadata[M] =
    RDDHistogramEqualization.singleband(self)

  /**
    * Given a histogram derived form the source RDD of Tile objects,
    * equalize the respective histograms of the RDD of tiles.
    *
    * @param  histogram  A histogram
    */
  def equalize[T <: AnyVal](histogram: Histogram[T]): RDD[(K, Tile)] with Metadata[M] =
    RDDHistogramEqualization.singleband(self, histogram)
}
