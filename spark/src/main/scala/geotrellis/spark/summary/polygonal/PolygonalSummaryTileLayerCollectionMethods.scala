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

package geotrellis.spark.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.raster.summary.polygonal._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector._
import org.apache.spark.Partitioner
import org.apache.spark.rdd._

import scala.reflect.ClassTag

abstract class PolygonalSummaryTileLayerCollectionMethods[
  K,
  M: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[Seq[(K, Tile)] with Metadata[M]] {
  import Implicits._
  protected implicit val _sc: SpatialComponent[K]

  def polygonalSummary[T: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handler: TilePolygonalSummaryHandler[T]
  ): T =
    self
      .asRasters
      .map(_._2.asFeature)
      .polygonalSummary(polygon, zeroValue)(handler)

  def polygonalSummary[T: ClassTag](
    multiPolygon: MultiPolygon,
    zeroValue: T,
    handler: TilePolygonalSummaryHandler[T]
  ): T =
    self
      .asRasters
      .map(_._2.asFeature)
      .polygonalSummary(multiPolygon, zeroValue)(handler)

  def polygonalSummaryByKey[T: ClassTag, L: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handler: TilePolygonalSummaryHandler[T],
    fKey: K => L
  ): Seq[(L, T)] =
    self
      .asRasters
      .map { case (key, raster) => (fKey(key), raster.asFeature) }
      .polygonalSummaryByKey(polygon, zeroValue)(handler)

  def polygonalSummaryByKey[T: ClassTag, L: ClassTag](
    multiPolygon: MultiPolygon,
    zeroValue: T,
    handler: TilePolygonalSummaryHandler[T],
    fKey: K => L
  ): Seq[(L, T)] =
    self
      .asRasters
      .map { case (key, raster) => (fKey(key), raster.asFeature) }
      .polygonalSummaryByKey(multiPolygon, zeroValue)(handler)

  def polygonalHistogram(polygon: Polygon): Histogram[Int] =
    polygonalSummary(polygon, FastMapHistogram(), IntHistogramSummary)

  def polygonalHistogram(multiPolygon: MultiPolygon): Histogram[Int] =
    polygonalSummary(multiPolygon, FastMapHistogram(), IntHistogramSummary)

  def polygonalHistogramDouble(polygon: Polygon): Histogram[Double] =
    polygonalSummary(polygon, StreamingHistogram(), DoubleHistogramSummary)

  def polygonalHistogramDouble(multiPolygon: MultiPolygon): Histogram[Double] =
    polygonalSummary(multiPolygon, StreamingHistogram(), DoubleHistogramSummary)

  def polygonalMax(polygon: Polygon): Int =
    if (self.isEmpty)
      NODATA
    else
      polygonalSummary(polygon, Int.MinValue, MaxSummary)

  def polygonalMax(multiPolygon: MultiPolygon): Int =
    if (self.isEmpty)
      NODATA
    else
      polygonalSummary(multiPolygon, Int.MinValue, MaxSummary)

  def polygonalMaxDouble(polygon: Polygon): Double =
    if (self.isEmpty)
      doubleNODATA
    else
      polygonalSummary(polygon, Double.MinValue, MaxDoubleSummary)

  def polygonalMaxDouble(multiPolygon: MultiPolygon): Double =
    if (self.isEmpty)
      doubleNODATA
    else
      polygonalSummary(multiPolygon, Double.MinValue, MaxDoubleSummary)

  def polygonalMin(polygon: Polygon): Int =
    if (self.isEmpty)
      NODATA
    else
      polygonalSummary(polygon, Int.MaxValue, MinSummary)

  def polygonalMin(multiPolygon: MultiPolygon): Int =
    if (self.isEmpty)
      NODATA
    else
      polygonalSummary(multiPolygon, Int.MaxValue, MinSummary)

  def polygonalMinDouble(polygon: Polygon): Double =
    if (self.isEmpty)
      doubleNODATA
    else
      polygonalSummary(polygon, Double.MaxValue, MinDoubleSummary)

  def polygonalMinDouble(multiPolygon: MultiPolygon): Double =
    if (self.isEmpty)
      doubleNODATA
    else
      polygonalSummary(multiPolygon, Double.MaxValue, MinDoubleSummary)

  def polygonalMean(polygon: Polygon): Double =
    if (self.isEmpty)
      doubleNODATA
    else
      polygonalSummary(polygon, MeanResult(0.0, 0L), MeanSummary).mean

  def polygonalMean(multiPolygon: MultiPolygon): Double =
    if (self.isEmpty)
      doubleNODATA
    else
      polygonalSummary(multiPolygon, MeanResult(0.0, 0L), MeanSummary).mean

  def polygonalSum(polygon: Polygon): Long =
    if (self.isEmpty)
      0L
    else
      polygonalSummary(polygon, 0L, SumSummary)

  def polygonalSum(multiPolygon: MultiPolygon): Long =
    if (self.isEmpty)
      0L
    else
      polygonalSummary(multiPolygon, 0L, SumSummary)

  def polygonalSumDouble(polygon: Polygon): Double =
    if (self.isEmpty)
      0.0
    else
      polygonalSummary(polygon, 0.0, SumDoubleSummary)

  def polygonalSumDouble(multiPolygon: MultiPolygon): Double =
    if (self.isEmpty)
      0.0
    else
      polygonalSummary(multiPolygon, 0.0, SumDoubleSummary)

}
