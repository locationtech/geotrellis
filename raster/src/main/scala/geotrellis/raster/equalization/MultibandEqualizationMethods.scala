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

package geotrellis.raster.equalization

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.MultibandTile
import geotrellis.util.MethodExtensions


trait MultibandEqualizationMethods extends MethodExtensions[MultibandTile] {

  /**
    * Equalize the histograms of the bands of this [[MultibandTile]].
    *
    * @param  histogram  A sequence of [[StreamingHistogram]] objects, one for each band
    * @return            A multiband tile whose bands have equalized histograms
    */
  def equalize(histograms: Array[StreamingHistogram]): MultibandTile = HistogramEqualization(self, histograms)

  /**
    * Equalize the histograms of the bands of this [[MultibandTile]].
    *
    * @return  A multiband tile whose bands have equalized histograms
    */
  def equalize(): MultibandTile = HistogramEqualization(self)
}
