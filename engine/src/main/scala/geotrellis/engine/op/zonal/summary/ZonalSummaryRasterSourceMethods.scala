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

package geotrellis.engine.op.zonal.summary

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.summary.polygonal._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.rasterize._
import geotrellis.vector._

import scala.collection.mutable
import spire.syntax.cfor._

@deprecated("geotrellis-engine has been deprecated", "7b92cb2")
trait ZonalSummaryRasterSourceMethods extends RasterSourceMethods {
  def mapIntersecting[B, That](p: Polygon)
                              (handler: TilePolygonalSummaryHandler[B]): DataSource[B, _] =
    _mapIntersecting(p, None)(handler.handleFullTile)(handler.handlePartialTile)

  def mapIntersecting[B, That](p: Polygon, fullTileResults: DataSource[B, _])
                               (handler: TilePolygonalSummaryHandler[B]): DataSource[B, _] =
    _mapIntersecting(p, Some(fullTileResults))(handler.handleFullTile)(handler.handlePartialTile)

  def mapIntersecting[B, That](p: Polygon, fullTileResults: Option[DataSource[B, _]])
                               (handler: TilePolygonalSummaryHandler[B]): DataSource[B, _] =
    _mapIntersecting(p, fullTileResults)(handler.handleFullTile)(handler.handlePartialTile)


  private
  def _mapIntersecting[B, That](p: Polygon, fullTileResults: Option[DataSource[B, _]])
                                (handleFullTile: Tile => B)(handlePartialTile: (Raster[Tile], Polygon) => B): DataSource[B, _] = {
    val newOp =
      (rasterSource.rasterDefinition, rasterSource.tiles).map { (rd, tiles) =>
        val tileExtents = TileExtents(rd.rasterExtent.extent, rd.tileLayout)
        val tileCols = rd.tileLayout.layoutCols
        val tileRows = rd.tileLayout.layoutRows
        val filtered = mutable.ListBuffer[Op[B]]()

        val _handleFullTile: Int => Op[B] =
          fullTileResults match {
            case Some(cached) =>
              { (i: Int) => cached.elements.flatMap(_(i)) }
            case None =>
              { (i: Int) =>
                tiles(i).map { t: Tile =>
                  handleFullTile(t)
                }
              }
          }

        cfor(0)(_ < tileRows, _ + 1) { row =>
          cfor(0)(_ < tileCols, _ + 1) { col =>
            val extent = tileExtents(col, row)

            if(p.contains(extent)) {
              filtered += _handleFullTile(row*tileCols + col)
            } else {
              p.intersection(extent) match {
                case PolygonResult(intersectionPoly) =>
                  filtered += tiles(row * tileCols + col).map { t =>
                    handlePartialTile(Raster(t, extent), intersectionPoly)
                  }
                case MultiPolygonResult(mp) =>
                  for(p <- mp.polygons) {
                    filtered += tiles(row*tileCols + col).map { t =>
                      handlePartialTile(Raster(t, extent), p)
                    }
                  }
                case _ => //No match? No Problem!
              }
            }
          }
        }
        filtered.toSeq
      }

    SeqSource(newOp)
  }

  def zonalSummary[T](
                       handler: TilePolygonalSummaryHandler[T],
                       p: Polygon,
                       cachedResult: Option[DataSource[T, _]]
  ): ValueSource[T] =
    mapIntersecting(p, cachedResult)(handler)
      .converge(handler.combineResults)

  def zonalHistogram(p: Polygon): ValueSource[Histogram[Int]] =
    zonalSummary(HistogramSummary, p, None)

  def zonalHistogram(p: Polygon, cached: DataSource[Histogram[Int], _]): ValueSource[Histogram[Int]] =
    zonalSummary(HistogramSummary, p, Some(cached))

  def zonalSum(p: Polygon): ValueSource[Long] =
    zonalSummary(SumSummary, p, None)

  def zonalSum(p: Polygon, cached: DataSource[Long, _]): ValueSource[Long] =
    zonalSummary(SumSummary, p, Some(cached))

  def zonalSumDouble(p: Polygon): ValueSource[Double] =
    zonalSummary(SumDoubleSummary, p, None)

  def zonalSumDouble(p: Polygon, cached: DataSource[Double, _]): ValueSource[Double] =
    zonalSummary(SumDoubleSummary, p, Some(cached))

  def zonalMin(p: Polygon): ValueSource[Int] =
    zonalSummary(MinSummary, p, None)

  def zonalMin(p: Polygon, cached: DataSource[Int, _]): ValueSource[Int] =
    zonalSummary(MinSummary, p, Some(cached))

  def zonalMinDouble(p: Polygon): ValueSource[Double] =
    zonalSummary(MinDoubleSummary, p, None)

  def zonalMinDouble(p: Polygon, cached: DataSource[Double, _]): ValueSource[Double] =
    zonalSummary(MinDoubleSummary, p, Some(cached))

  def zonalMax(p: Polygon): ValueSource[Int] =
    zonalSummary(MaxSummary, p, None)

  def zonalMax(p: Polygon, cached: DataSource[Int, _]): ValueSource[Int] =
    zonalSummary(MaxSummary, p, Some(cached))

  def zonalMaxDouble(p: Polygon): ValueSource[Double] =
    zonalSummary(MaxDoubleSummary, p, None)

  def zonalMaxDouble(p: Polygon, cached: DataSource[Double, _]): ValueSource[Double] =
    zonalSummary(MaxDoubleSummary, p, Some(cached))

  def zonalMean(p: Polygon): ValueSource[Double] =
    zonalSummary(MeanSummary, p, None).map(_.mean)

  def zonalMean(p: Polygon, cached: DataSource[MeanResult, _]): ValueSource[Double] =
    zonalSummary(MeanSummary, p, Some(cached)).map(_.mean)
}
