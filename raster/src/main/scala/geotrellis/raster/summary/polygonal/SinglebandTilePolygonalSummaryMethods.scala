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

package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.util.MethodExtensions
import geotrellis.vector._



/**
  * Trait containing extension methods for doing polygonal summaries
  * on tiles.
  */
trait SinglebandTilePolygonalSummaryMethods extends MethodExtensions[Tile] {

  /**
    * Given a Polygon, an Extent, and a summary handler, generate the
    * summary of a polygonal area with respect to the present tile.
    */
  def polygonalSummary[T](extent: Extent, polygon: Polygon, handler: TilePolygonalSummaryHandler[T]): T = {
    val results = {
      if(polygon.contains(extent)) {
        Seq(handler.handleFullTile(self))
      } else {
        polygon.intersection(extent) match {
          case PolygonResult(intersection) =>
            Seq(handler.handlePartialTile(Raster(self, extent), intersection))
          case MultiPolygonResult(mp) =>
            mp.polygons.map { intersection =>
              handler.handlePartialTile(Raster(self, extent), intersection)
            }
          case _ => Seq()
        }
      }
    }

    handler.combineResults(results)
  }

  /**
    * Given a MultiPolygon, an Extent, and a summary handler, generate
    * the summary of a polygonal area with respect to the present
    * tile.
    */
  def polygonalSummary[T](extent: Extent, multiPolygon: MultiPolygon, handler: TilePolygonalSummaryHandler[T]): T = {
    val results = {
      if(multiPolygon.contains(extent)) {
        Seq(handler.handleFullTile(self))
      } else {
        multiPolygon.intersection(extent) match {
          case PolygonResult(intersection) =>
            Seq(handler.handlePartialTile(Raster(self, extent), intersection))
          case MultiPolygonResult(mp) =>
            mp.polygons.map { intersection =>
              handler.handlePartialTile(Raster(self, extent), intersection)
            }
          case _ => Seq()
        }
      }
    }

    handler.combineResults(results)
  }

  /**
    * Given an extent and a Polygon, compute the histogram of the tile
    * values contained within.
    */
  def polygonalHistogram(extent: Extent, geom: Polygon): Histogram[Int] =
    polygonalSummary(extent, geom, IntHistogramSummary)

  /**
    * Given an extent and a MultiPolygon, compute the histogram of the
    * tile values contained within.
    */
  def polygonalHistogram(extent: Extent, geom: MultiPolygon): Histogram[Int] =
    polygonalSummary(extent, geom, IntHistogramSummary)

  /**
    * Given an extent and a Polygon, compute the histogram of the tile
    * values contained within.
    */
  def polygonalHistogramDouble(extent: Extent, geom: Polygon): Histogram[Double] =
    polygonalSummary(extent, geom, DoubleHistogramSummary)

  /**
    * Given an extent and a MultiPolygon, compute the histogram of the
    * tile values contained within.
    */
  def polygonalHistogramDouble(extent: Extent, geom: MultiPolygon): Histogram[Double] =
    polygonalSummary(extent, geom, DoubleHistogramSummary)

  /**
    * Given an extent and a Polygon, compute the maximum of the tile
    * values contained within.
    */
  def polygonalMax(extent: Extent, geom: Polygon): Int =
    polygonalSummary(extent, geom, MaxSummary)

  /**
    * Given an extent and a MultiPolygon, compute the maximum of the
    * tile values contained within.
    */
  def polygonalMax(extent: Extent, geom: MultiPolygon): Int =
    polygonalSummary(extent, geom, MaxSummary)

  /**
    * Given an extent and a Polygon, compute the maximum of the tile
    * values contained within.
    */
  def polygonalMaxDouble(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, MaxDoubleSummary)

  /**
    * Given an extent and a MultiPolygon, compute the maximum of the
    * tile values contained within.
    */
  def polygonalMaxDouble(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, MaxDoubleSummary)

  /**
    * Given an extent and a Polygon, compute the minimum of the tile
    * values contained within.
    */
  def polygonalMin(extent: Extent, geom: Polygon): Int =
    polygonalSummary(extent, geom, MinSummary)

  /**
    * Given an extent and a MultiPolygon, compute the minimum of the
    * tile values contained within.
    */
  def polygonalMin(extent: Extent, geom: MultiPolygon): Int =
    polygonalSummary(extent, geom, MinSummary)

  /**
    * Given an extent and a Polygon, compute the minimum of the tile
    * values contained within.
    */
  def polygonalMinDouble(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, MinDoubleSummary)

  /**
    * Given an extent and a MultiPolygon, compute the minimum of the
    * tile values contained within.
    */
  def polygonalMinDouble(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, MinDoubleSummary)

  /**
    * Given an extent and a Polygon, compute the mean of the tile
    * values contained within.
    */
  def polygonalMean(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, MeanSummary).mean

  /**
    * Given an extent and a MultiPolygon, compute the mean of the tile
    * values contained within.
    */
  def polygonalMean(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, MeanSummary).mean

  /**
    * Given an extent and a Polygon, compute the sum of the tile
    * values contained within.
    */
  def polygonalSum(extent: Extent, geom: Polygon): Long =
    polygonalSummary(extent, geom, SumSummary)

  /**
    * Given an extent and a MultiPolygon, compute the sum of the tile
    * values contained within.
    */
  def polygonalSum(extent: Extent, geom: MultiPolygon): Long =
    polygonalSummary(extent, geom, SumSummary)

  /**
    * Given an extent and a Polygon, compute the sum of the tile
    * values contained within.
    */
  def polygonalSumDouble(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, SumDoubleSummary)

  /**
    * Given an extent and a MultiPolygon, compute the sum of the tile
    * values contained within.
    */
  def polygonalSumDouble(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, SumDoubleSummary)

}
