/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.timeseries

import geotrellis.vector._
import geotrellis.tiling.SpaceTimeKey
import geotrellis.raster._
import geotrellis.raster.summary.polygonal._
import geotrellis.layers.mask.Mask.Options
import geotrellis.spark._
import geotrellis.util.annotations.experimental
import geotrellis.util.MethodExtensions

import java.time.ZonedDateTime


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object RDDTimeSeriesFunctions {

  /**
    * $experimental
    */
  @experimental def histogramProjection(tile: Tile): StreamingHistogram =
    StreamingHistogram.fromTile(tile)

  /**
    * $experimental
    */
  @experimental def histogramReduction(left: StreamingHistogram, right: StreamingHistogram): StreamingHistogram =
    left + right

  /**
    * $experimental
    */
  @experimental def meanReduction(left: MeanResult, right: MeanResult): MeanResult =
    left + right

  /**
    * $experimental
    */
  @experimental def maxReduction(left: Double, right: Double): Double =
    scala.math.max(left, right)

  /**
    * $experimental
    */
  @experimental def minReduction(left: Double, right: Double): Double =
    scala.math.min(left, right)

  /**
    * $experimental
    */
  @experimental def sumReduction(left: Double, right: Double): Double =
    left + right
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental abstract class RDDTimeSeriesMethods
    extends MethodExtensions[TileLayerRDD[SpaceTimeKey]] {

  /**
    * $experimental
    */
  @experimental def sumSeries(
    polygon: MultiPolygon,
    options: Options
  ): Map[ZonedDateTime, Double] =
    sumSeries(List(polygon), options)

  /**
    * $experimental
    */
  @experimental def sumSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    sumSeries(List(polygon), Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def sumSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    sumSeries(polygons, Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def sumSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      SumDoubleSummary.handleFullTile,
      RDDTimeSeriesFunctions.sumReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

  /**
    * $experimental
    */
  @experimental def minSeries(
    polygon: MultiPolygon,
    options: Options
  ): Map[ZonedDateTime, Double] =
    minSeries(List(polygon), options)

  /**
    * $experimental
    */
  @experimental def minSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    minSeries(List(polygon), Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def minSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    minSeries(polygons, Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def minSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      MinDoubleSummary.handleFullTile,
      RDDTimeSeriesFunctions.minReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

  /**
    * $experimental
    */
  @experimental def maxSeries(
    polygon: MultiPolygon,
    options: Options
  ): Map[ZonedDateTime, Double] =
    maxSeries(List(polygon), options)

  /**
    * $experimental
    */
  @experimental def maxSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    maxSeries(List(polygon), Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def maxSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    maxSeries(polygons, Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def maxSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      MaxDoubleSummary.handleFullTile,
      RDDTimeSeriesFunctions.maxReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

  /**
    * $experimental
    */
  @experimental def meanSeries(
    polygon: MultiPolygon,
    options: Options = Options.DEFAULT
  ): Map[ZonedDateTime, Double] =
    meanSeries(List(polygon), options)

  /**
    * $experimental
    */
  @experimental def meanSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    meanSeries(polygon, Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def meanSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    meanSeries(polygons, Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def meanSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      MeanResult.fromFullTileDouble,
      RDDTimeSeriesFunctions.meanReduction,
      polygons,
      options
    )
      .mapValues({ mr => mr.mean })
      .collect()
      .toMap
  }

  /**
    * $experimental
    */
  @experimental def histogramSeries(
    polygon: MultiPolygon,
    options: Options = Options.DEFAULT
  ): Map[ZonedDateTime, Histogram[Double]] =
    histogramSeries(List(polygon), options)

  /**
    * $experimental
    */
  @experimental def histogramSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Histogram[Double]] =
    histogramSeries(polygon, Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def histogramSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Histogram[Double]] =
    histogramSeries(polygons, Options.DEFAULT)

  /**
    * $experimental
    */
  @experimental def histogramSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Histogram[Double]] = {
    TimeSeries(
      self,
      RDDTimeSeriesFunctions.histogramProjection,
      RDDTimeSeriesFunctions.histogramReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

}
