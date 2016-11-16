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

package geotrellis.raster.matching

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.MultibandTile
import geotrellis.util.MethodExtensions


trait MultibandMatchingMethods extends MethodExtensions[MultibandTile] {

  def matchHistogram(targetHistograms: Seq[StreamingHistogram]): MultibandTile =
    HistogramMatching(self, targetHistograms)

  def matchHistogram(
    sourceHistograms: Seq[StreamingHistogram],
    targetHistograms: Seq[StreamingHistogram]
  ): MultibandTile = HistogramMatching(self, sourceHistograms, targetHistograms)
}
