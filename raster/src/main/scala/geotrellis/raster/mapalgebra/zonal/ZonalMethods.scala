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

package geotrellis.raster.mapalgebra.zonal

import geotrellis.raster._
import geotrellis.raster.summary._
import geotrellis.raster.histogram._
import geotrellis.util.MethodExtensions


trait ZonalMethods extends MethodExtensions[Tile] {
  def zonalHistogramInt(zones: Tile): Map[Int, Histogram[Int]] =
    IntZonalHistogram(self, zones)

  def zonalStatisticsInt(zones: Tile): Map[Int, Statistics[Int]] =
    IntZonalHistogram(self, zones)
      .map { case (zone: Int, hist: Histogram[Int]) => (zone -> hist.statistics.get) }
      .toMap

  def zonalHistogramDouble(zones: Tile): Map[Int, Histogram[Double]] =
    DoubleZonalHistogram(self, zones)

  def zonalStatisticsDouble(zones: Tile): Map[Int, Statistics[Double]] =
    DoubleZonalHistogram(self, zones)
      .map { case (zone: Int, hist: Histogram[Double]) => (zone -> hist.statistics.get) }
      .toMap

  def zonalPercentage(zones: Tile): Tile =
    ZonalPercentage(self, zones)
}
