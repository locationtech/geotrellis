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
import geotrellis.raster.op.zonal.summary._
import geotrellis.raster.stats._
import geotrellis.raster.rasterize._
import geotrellis.vector._

import scala.collection.mutable
import spire.syntax.cfor._

trait ZonalSummaryRasterSourceMethods extends RasterSourceMethods {
  def mapIntersecting[B, That](p: Polygon)
                              (handleTileIntersection: TileIntersection=>B): DataSource[B, _] =
    _mapIntersecting(p, None)(handleTileIntersection)

  def mapIntersecting[B, That](p: Polygon, fullTileResults: DataSource[B, _])
                               (handleTileIntersection: TileIntersection=>B): DataSource[B, _] = 
    _mapIntersecting(p, Some(fullTileResults))(handleTileIntersection)

  def mapIntersecting[B, That](p: Polygon, fullTileResults: Option[DataSource[B, _]])
                               (handleTileIntersection: TileIntersection=>B): DataSource[B, _] = 
    _mapIntersecting(p, fullTileResults)(handleTileIntersection)


  private 
  def _mapIntersecting[B, That](p: Polygon, fullTileResults: Option[DataSource[B, _]])
                                (handleTileIntersection: TileIntersection=>B): DataSource[B, _] = {
    val newOp = 
      (rasterSource.rasterDefinition, rasterSource.tiles).map { (rd, tiles) =>
        val tileExtents = TileExtents(rd.rasterExtent.extent, rd.tileLayout)
        val tileCols = rd.tileLayout.layoutCols
        val tileRows = rd.tileLayout.layoutRows
        val filtered = mutable.ListBuffer[Op[B]]()

        val handleFullTile: Int => Op[B] =
          fullTileResults match {
            case Some(cached) =>
              { (i: Int) => cached.elements.flatMap(_(i)) }
            case None =>
              { (i: Int) => 
                tiles(i).map { t =>
                  handleTileIntersection(FullTileIntersection(t))
                }
              }
          }

        cfor(0)(_ < tileCols, _ + 1) { col =>
          cfor(0)(_ < tileRows, _ + 1) { row =>
            val extent = tileExtents(col, row)

            if(p.contains(extent)) {
              filtered += handleFullTile(row*tileCols + col)
            } else {
              p.intersection(extent) match {
                case PolygonResult(intersectionPoly) =>
                  filtered += tiles(row * tileCols + col).map { t =>
                    handleTileIntersection(
                      PartialTileIntersection(
                        t, 
                        extent,
                        intersectionPoly
                      )
                    )
                  }
                case MultiPolygonResult(mp) =>
                  for(p <- mp.polygons) {
                    filtered += tiles(row*tileCols + col).map { t =>
                      val pti =
                        PartialTileIntersection(
                          t,
                          extent,
                          p
                        )
                      handleTileIntersection(pti)
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

  def zonalSummary[T, U](
    handler: TileIntersectionHandler[T, U], 
    p: Polygon, 
    cachedResult: Option[DataSource[T, _]]
  ): ValueSource[U] =
    mapIntersecting(p, cachedResult)(handler)
      .converge(handler.combineResults)

  def zonalHistogram(p: Polygon): ValueSource[Histogram] =
    zonalSummary(Histogram, p, None)

  def zonalHistogram(p: Polygon, cached: DataSource[Histogram, _]): ValueSource[Histogram] =
    zonalSummary(Histogram, p, Some(cached))

  def zonalSum(p: Polygon): ValueSource[Long] =
    zonalSummary(Sum, p, None)

  def zonalSum(p: Polygon, cached: DataSource[Long, _]): ValueSource[Long] =
    zonalSummary(Sum, p, Some(cached))

  def zonalSumDouble(p: Polygon): ValueSource[Double] =
    zonalSummary(SumDouble, p, None)

  def zonalSumDouble(p: Polygon, cached: DataSource[Double, _]): ValueSource[Double] =
    zonalSummary(SumDouble, p, Some(cached))

  def zonalMin(p: Polygon): ValueSource[Int] =
    zonalSummary(Min, p, None)

  def zonalMin(p: Polygon, cached: DataSource[Int, _]): ValueSource[Int] =
    zonalSummary(Min, p, Some(cached))

  def zonalMinDouble(p: Polygon): ValueSource[Double] =
    zonalSummary(MinDouble, p, None)

  def zonalMinDouble(p: Polygon, cached: DataSource[Double, _]): ValueSource[Double] =
    zonalSummary(MinDouble, p, Some(cached))

  def zonalMax(p: Polygon): ValueSource[Int] =
    zonalSummary(Max, p, None)

  def zonalMax(p: Polygon, cached: DataSource[Int, _]): ValueSource[Int] =
    zonalSummary(Max, p, Some(cached))

  def zonalMaxDouble(p: Polygon): ValueSource[Double] =
    zonalSummary(MaxDouble, p, None)

  def zonalMaxDouble(p: Polygon, cached: DataSource[Double, _]): ValueSource[Double] =
    zonalSummary(MaxDouble, p, Some(cached))

  def zonalMean(p: Polygon): ValueSource[Double] =
    zonalSummary(Mean, p, None)

  def zonalMean(p: Polygon, cached: DataSource[MeanResult, _]): ValueSource[Double] =
    zonalSummary(Mean, p, Some(cached))
}
