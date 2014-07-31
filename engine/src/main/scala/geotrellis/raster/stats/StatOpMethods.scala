/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.stats

import geotrellis.raster._
import geotrellis.engine._

trait StatOpMethods[+Repr <: RasterSource] { self: Repr =>
  private def convergeHistograms(histograms: Seq[Histogram]): Histogram = FastMapHistogram.fromHistograms(histograms)

  def tileHistograms(): DataSource[Histogram, Histogram] = this map (_.histogram) withConverge(convergeHistograms)
  def histogram(): ValueSource[Histogram] = this map(_.histogram) converge(convergeHistograms)
  def statistics(): ValueSource[Statistics] = histogram().map{ h => h.generateStatistics() }
  def classBreaks(numBreaks: Int): ValueSource[Array[Int]] = histogram map (_.getQuantileBreaks(numBreaks))
}
