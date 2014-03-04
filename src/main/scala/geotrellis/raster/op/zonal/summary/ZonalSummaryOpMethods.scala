/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.statistics._

import scala.collection.mutable
import scalaxy.loops._

abstract sealed trait TileIntersection

case class PartialTileIntersection[D](tile:Raster,intersections:List[Polygon[D]]) extends TileIntersection
case class FullTileIntersection(tile:Raster) extends TileIntersection

trait ZonalSummaryOpMethods[+Repr <: RasterSource] { self:Repr =>
  def mapIntersecting[B,That,D](p:Op[feature.Polygon[D]])
                               (handleTileIntersection:TileIntersection=>B)
                               (implicit bf:CanBuildSourceFrom[Repr,B,That]):That =
    _mapIntersecting(p,None)(handleTileIntersection)(bf.apply(this))

  def mapIntersecting[B,That,D](p:Op[feature.Polygon[D]],fullTileResults:DataSource[B,_])
                               (handleTileIntersection:TileIntersection=>B)
                               (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = 
    _mapIntersecting(p,Some(fullTileResults))(handleTileIntersection)(bf.apply(this))

  def mapIntersecting[B,That,D](p:Op[feature.Polygon[D]],fullTileResults:Option[DataSource[B,_]])
                               (handleTileIntersection:TileIntersection=>B)
                               (implicit bf:CanBuildSourceFrom[Repr,B,That]):That = 
    _mapIntersecting(p,fullTileResults)(handleTileIntersection)(bf.apply(this))


  private 
  def _mapIntersecting[B,That,D](p:Op[feature.Polygon[D]],fullTileResults:Option[DataSource[B,_]])
                                (handleTileIntersection:TileIntersection=>B)
                                (builder:SourceBuilder[B,That]):That = {
    val newOp = 
      (rasterDefinition,tiles,p).map { (rd,tiles,p) =>
        val rl = rd.tileLayout.getResolutionLayout(rd.rasterExtent)
        val tileCols = rd.tileLayout.tileCols
        val tileRows = rd.tileLayout.tileRows
        val filtered = mutable.ListBuffer[Op[B]]()

        val handleFullTile:Int => Op[B] =
          fullTileResults match {
            case Some(cached) =>
              { (i:Int) => cached.elements.flatMap(_(i)) }
            case None =>
              { (i:Int) => 
                tiles(i).map { t =>
                  handleTileIntersection(FullTileIntersection(t))
                }
              }
          }

        for(col <- 0 until tileCols optimized) {
          for(row <- 0 until tileRows optimized) {
            val tilePoly =
              rl.getRasterExtent(col,row)
                .extent
                .asFeature()
                .geom

            if(p.geom.contains(tilePoly)) {              
              filtered += handleFullTile(row*tileCols + col)
            } else {
              val intersections = tilePoly.intersection(p.geom).asPolygonSet.map(Polygon(_,0))
              if(!intersections.isEmpty) {
                filtered += tiles(row*tileCols + col).map { t =>
                  handleTileIntersection(PartialTileIntersection(t,intersections))
                }
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
  def zonalSummary[T,V,That <: DataSource[_,V],D]
    (tileSummary:TileSummary[T,V,That], p:Op[Polygon[D]], cachedResult:Option[DataSource[T,_]]) =
    tileSummary.converge {
      self.mapIntersecting(p,cachedResult) { tileIntersection =>
        tileIntersection match {
          case ft @ FullTileIntersection(_) => tileSummary.handleFullTile(ft)
          case pt @ PartialTileIntersection(_,_) => tileSummary.handlePartialTile(pt)
        }
      }
    }

  def zonalHistogram[D](p:Op[feature.Polygon[D]]):ValueSource[Histogram] =
    zonalSummary(Histogram,p,None)

  def zonalHistogram[D](p:Op[feature.Polygon[D]],cached:DataSource[Histogram,_]):ValueSource[Histogram] =
    zonalSummary(Histogram,p,Some(cached))

  def zonalSum[D](p:Op[feature.Polygon[D]]):ValueSource[Long] =
    zonalSummary(Sum,p,None)

  def zonalSum[D](p:Op[feature.Polygon[D]],cached:DataSource[Long,_]):ValueSource[Long] =
    zonalSummary(Sum,p,Some(cached))

  def zonalSumDouble[D](p:Op[feature.Polygon[D]]):ValueSource[Double] =
    zonalSummary(SumDouble,p,None)

  def zonalSumDouble[D](p:Op[feature.Polygon[D]],cached:DataSource[Double,_]):ValueSource[Double] =
    zonalSummary(SumDouble,p,Some(cached))

  def zonalMin[D](p:Op[feature.Polygon[D]]):ValueSource[Int] =
    zonalSummary(Min,p,None)

  def zonalMin[D](p:Op[feature.Polygon[D]],cached:DataSource[Int,_]):ValueSource[Int] =
    zonalSummary(Min,p,Some(cached))

  def zonalMinDouble[D](p:Op[feature.Polygon[D]]):ValueSource[Double] =
    zonalSummary(MinDouble,p,None)

  def zonalMinDouble[D](p:Op[feature.Polygon[D]],cached:DataSource[Double,_]):ValueSource[Double] =
    zonalSummary(MinDouble,p,Some(cached))

  def zonalMax[D](p:Op[feature.Polygon[D]]):ValueSource[Int] =
    zonalSummary(Max,p,None)

  def zonalMax[D](p:Op[feature.Polygon[D]],cached:DataSource[Int,_]):ValueSource[Int] =
    zonalSummary(Max,p,Some(cached))

  def zonalMaxDouble[D](p:Op[feature.Polygon[D]]):ValueSource[Double] =
    zonalSummary(MaxDouble,p,None)

  def zonalMaxDouble[D](p:Op[feature.Polygon[D]],cached:DataSource[Double,_]):ValueSource[Double] =
    zonalSummary(MaxDouble,p,Some(cached))

  def zonalMean[D](p:Op[feature.Polygon[D]]):ValueSource[Double] =
    zonalSummary(Mean,p,None)

  def zonalMean[D](p:Op[feature.Polygon[D]],cached:DataSource[MeanResult,_]):ValueSource[Double] =
    zonalSummary(Mean,p,Some(cached))

  def zonalMeanDouble[D](p:Op[feature.Polygon[D]]):ValueSource[Double] =
    zonalSummary(MeanDouble,p,None)

  def zonalMeanDouble[D](p:Op[feature.Polygon[D]],cached:DataSource[MeanResult,_]):ValueSource[Double] =
    zonalSummary(MeanDouble,p,Some(cached))
}
