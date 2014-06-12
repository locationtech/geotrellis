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

package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.stats._
import geotrellis.raster.rasterize._
import geotrellis.feature._
import geotrellis.engine._

import scala.collection.mutable
import spire.syntax.cfor._

abstract sealed trait TileIntersection

case class PartialTileIntersection(tile: Tile, rasterExtent: RasterExtent, intersections: Seq[Polygon]) 
    extends TileIntersection
case class FullTileIntersection(tile: Tile) extends TileIntersection

trait ZonalSummaryOpMethods[+Repr <: RasterSource] { self: Repr =>
  def mapIntersecting[B, That](p: Polygon)
                               (handleTileIntersection: TileIntersection=>B)
                               (implicit bf: CanBuildSourceFrom[Repr, B, That]): That =
    _mapIntersecting(p, None)(handleTileIntersection)(bf.apply(this))

  def mapIntersecting[B, That](p: Polygon, fullTileResults: DataSource[B, _])
                               (handleTileIntersection: TileIntersection=>B)
                               (implicit bf: CanBuildSourceFrom[Repr, B, That]): That = 
    _mapIntersecting(p, Some(fullTileResults))(handleTileIntersection)(bf.apply(this))

  def mapIntersecting[B, That](p: Polygon, fullTileResults: Option[DataSource[B, _]])
                               (handleTileIntersection: TileIntersection=>B)
                               (implicit bf: CanBuildSourceFrom[Repr, B, That]): That = 
    _mapIntersecting(p, fullTileResults)(handleTileIntersection)(bf.apply(this))


  private 
  def _mapIntersecting[B, That](p: Polygon, fullTileResults: Option[DataSource[B, _]])
                                (handleTileIntersection: TileIntersection=>B)
                                (builder: SourceBuilder[B, That]): That = {
    val newOp = 
      (rasterDefinition, tiles).map { (rd, tiles) =>
        val rl = rd.tileLayout.getResolutionLayout(rd.rasterExtent)
        val tileCols = rd.tileLayout.tileCols
        val tileRows = rd.tileLayout.tileRows
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
            val rasterExtent = rl.getRasterExtent(col, row)
            val tilePoly =
              rasterExtent
                .extent
                .toPolygon

            if(p.contains(tilePoly)) {
              filtered += handleFullTile(row*tileCols + col)
            } else {
              tilePoly.intersection(p) match {
                case PolygonResult(intersectionPoly) =>
                  filtered += tiles(row*tileCols + col).map { t =>
                    handleTileIntersection(PartialTileIntersection(t, rasterExtent, Seq(intersectionPoly)))
                  }
                case MultiPolygonResult(intersectionMultiPoly) =>
                  filtered += tiles(row*tileCols + col).map { t =>
                    val pti = PartialTileIntersection(t, rasterExtent, intersectionMultiPoly.polygons)
                    handleTileIntersection(pti)
                  }
                case _ => //No match? No Problem
              }
            }
          }
        }
        filtered.toSeq
      }

    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  private 
  def zonalSummary[T, V, That <: DataSource[_, V]](
    tileSummary: TileSummary[T, V, That], 
    p: Polygon, 
    cachedResult: Option[DataSource[T, _]]
  ) =
    tileSummary.converge {
      self.mapIntersecting(p, cachedResult) { tileIntersection =>
        tileIntersection match {
          case ft: FullTileIntersection => tileSummary.handleFullTile(ft)
          case pt: PartialTileIntersection => tileSummary.handlePartialTile(pt)
        }
      }
    }

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

  def zonalMeanDouble(p: Polygon): ValueSource[Double] =
    zonalSummary(MeanDouble, p, None)

  def zonalMeanDouble(p: Polygon, cached: DataSource[MeanResult, _]): ValueSource[Double] =
    zonalSummary(MeanDouble, p, Some(cached))
}
