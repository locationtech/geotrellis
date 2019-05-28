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

import geotrellis.layers.Metadata
import geotrellis.raster.summary.polygonal._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.vector._
import geotrellis.util._
import org.apache.spark.Partitioner
import org.apache.spark.rdd._

import scala.reflect.ClassTag

abstract class PolygonalSummaryMultibandTileLayerCollectionMethods[
  K: ClassTag,
  M: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[Seq[(K, MultibandTile)] with Metadata[M]] {
  import Implicits._
  protected implicit val _sc: SpatialComponent[K]

  def polygonalSummary[T: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handler: MultibandTilePolygonalSummaryHandler[T]
  ): T =
    self
      .asRasters
      .map(_._2.asFeature)
      .polygonalSummary(polygon, zeroValue)(handler)

  def polygonalSummary[T: ClassTag](
    multiPolygon: MultiPolygon,
    zeroValue: T,
    handler: MultibandTilePolygonalSummaryHandler[T]
  ): T =
    self
      .asRasters
      .map(_._2.asFeature)
      .polygonalSummary(multiPolygon, zeroValue)(handler)

  def polygonalSummaryByKey[T: ClassTag, L: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handler: MultibandTilePolygonalSummaryHandler[T],
    fKey: K => L
  ): Seq[(L, T)] =
    self
      .asRasters
      .map { case (key, raster) => (fKey(key), raster.asFeature) }
      .polygonalSummaryByKey(polygon, zeroValue)(handler)

  def polygonalSummaryByKey[T: ClassTag, L: ClassTag](
    multiPolygon: MultiPolygon,
    zeroValue: T,
    handler: MultibandTilePolygonalSummaryHandler[T],
    fKey: K => L
  ): Seq[(L, T)] =
    self
      .asRasters
      .map { case (key, raster) => (fKey(key), raster.asFeature) }
      .polygonalSummaryByKey(multiPolygon, zeroValue)(handler)

  def polygonalHistogram(polygon: Polygon): Array[Histogram[Int]] =
    polygonalSummary(polygon, Array(FastMapHistogram().asInstanceOf[Histogram[Int]]), MultibandTileIntHistogramSummary)

  def polygonalHistogram(multiPolygon: MultiPolygon): Array[Histogram[Int]] =
    polygonalSummary(multiPolygon, Array(FastMapHistogram().asInstanceOf[Histogram[Int]]), MultibandTileIntHistogramSummary)

  def polygonalHistogramDouble(polygon: Polygon): Array[Histogram[Double]] =
    polygonalSummary(polygon, Array(StreamingHistogram().asInstanceOf[Histogram[Double]]), MultibandTileDoubleHistogramSummary)

  def polygonalHistogramDouble(multiPolygon: MultiPolygon): Array[Histogram[Double]] =
    polygonalSummary(multiPolygon, Array(StreamingHistogram().asInstanceOf[Histogram[Double]]), MultibandTileDoubleHistogramSummary)

  def polygonalMax(polygon: Polygon): Array[Int] =
    if (self.isEmpty)
      Array(NODATA)
    else
      polygonalSummary(polygon, Array(Int.MinValue), MultibandTileMaxSummary)

  def polygonalMax(multiPolygon: MultiPolygon): Array[Int] =
    if (self.isEmpty)
      Array(NODATA)
    else
      polygonalSummary(multiPolygon, Array(Int.MinValue), MultibandTileMaxSummary)

  def polygonalMaxDouble(polygon: Polygon): Array[Double] =
    if (self.isEmpty)
      Array(doubleNODATA)
    else
      polygonalSummary(polygon, Array(Double.MinValue), MultibandTileMaxDoubleSummary)

  def polygonalMaxDouble(multiPolygon: MultiPolygon): Array[Double] =
    if (self.isEmpty)
      Array(doubleNODATA)
    else
      polygonalSummary(multiPolygon, Array(Double.MinValue), MultibandTileMaxDoubleSummary)

  def polygonalMin(polygon: Polygon): Array[Int] =
    if (self.isEmpty)
      Array(NODATA)
    else
      polygonalSummary(polygon, Array(Int.MaxValue), MultibandTileMinSummary)

  def polygonalMin(multiPolygon: MultiPolygon): Array[Int] =
    if (self.isEmpty)
      Array(NODATA)
    else
      polygonalSummary(multiPolygon, Array(Int.MaxValue), MultibandTileMinSummary)

  def polygonalMinDouble(polygon: Polygon): Array[Double] =
    if (self.isEmpty)
      Array(doubleNODATA)
    else
      polygonalSummary(polygon, Array(Double.MaxValue), MultibandTileMinDoubleSummary)

  def polygonalMinDouble(multiPolygon: MultiPolygon): Array[Double] =
    if (self.isEmpty)
      Array(doubleNODATA)
    else
      polygonalSummary(multiPolygon, Array(Double.MaxValue), MultibandTileMinDoubleSummary)

  def polygonalMean(polygon: Polygon): Array[Double] =
    if (self.isEmpty)
      Array(doubleNODATA)
    else
      polygonalSummary(polygon, Array(MeanResult(0.0, 0L)), MultibandTileMeanSummary) map { _.mean }

  def polygonalMean(multiPolygon: MultiPolygon): Array[Double] =
    if (self.isEmpty)
      Array(doubleNODATA)
    else
      polygonalSummary(multiPolygon, Array(MeanResult(0.0, 0L)), MultibandTileMeanSummary) map { _.mean }

  def polygonalSum(polygon: Polygon): Array[Long] =
    if (self.isEmpty)
      Array(0L)
    else
      polygonalSummary(polygon, Array(0L), MultibandTileSumSummary)

  def polygonalSum(multiPolygon: MultiPolygon): Array[Long] =
    if (self.isEmpty)
      Array(0L)
    else
      polygonalSummary(multiPolygon, Array(0L), MultibandTileSumSummary)

  def polygonalSumDouble(polygon: Polygon): Array[Double] =
    if (self.isEmpty)
      Array(0.0)
    else
      polygonalSummary(polygon, Array(0.0), MultibandTileSumDoubleSummary)

  def polygonalSumDouble(multiPolygon: MultiPolygon): Array[Double] =
    if (self.isEmpty)
      Array(0.0)
    else
      polygonalSummary(multiPolygon, Array(0.0), MultibandTileSumDoubleSummary)
}
