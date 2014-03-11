/***
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
 ***/

package geotrellis.source

import geotrellis._
import geotrellis.feature._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.render.op._
import geotrellis.process.RasterLayerInfo

import geotrellis.raster._

import scalaxy.loops._
import scala.collection.mutable

trait RasterSourceLike[+Repr <: RasterSource] 
    extends DataSourceLike[Raster,Raster, Repr]
    with DataSource[Raster,Raster] 
    with local.LocalOpMethods[Repr] 
    with focal.FocalOpMethods[Repr]
    with global.GlobalOpMethods[Repr]
    with zonal.ZonalOpMethods[Repr]
    with zonal.summary.ZonalSummaryOpMethods[Repr]
    with hydrology.HydrologyOpMethods[Repr]
    with stat.StatOpMethods[Repr] 
    with io.IoOpMethods[Repr] 
    with RenderOpMethods[Repr] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition:Op[RasterDefinition]

  def convergeOp():Op[Raster] =
    tiles.flatMap { ts =>
      if(ts.size == 1) { ts(0) }
      else { 
        (rasterDefinition,logic.Collect(ts)).map { (rd,tileSeq) =>
          TileRaster(tileSeq,rd.rasterExtent,rd.tileLayout).toArrayRaster
        }
      }
    }

  def global[That](f:Raster=>Raster)
                  (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps:Op[Seq[Op[Raster]]] =
      (rasterDefinition,logic.Collect(tiles)).map { (rd,tileSeq) =>
        if(rd.isTiled) {
          val r = f(TileRaster(tileSeq.toSeq, rd.rasterExtent,rd.tileLayout))
          TileRaster.split(r,rd.tileLayout).map(Literal(_))
        } else {
          Seq(f(tileSeq(0)))
        }
      }
    // Set into new RasterSource
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def globalOp[T,That](f:Raster=>Op[Raster])
                      (implicit bf:CanBuildSourceFrom[Repr,Raster,That]):That = {
    val tileOps:Op[Seq[Op[Raster]]] =
      (rasterDefinition,logic.Collect(tiles)).flatMap { (rd,tileSeq) =>
        if(rd.isTiled) {
          f(TileRaster(tileSeq.toSeq, rd.rasterExtent,rd.tileLayout)).map { r =>
            TileRaster.split(r,rd.tileLayout).map(Literal(_))
          }
        } else {
          Seq(f(tileSeq(0)))
        }
      }
    // Set into new RasterSource
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def convert(newType:RasterType) = {
    val newDef = rasterDefinition.map(_.withType(newType))
    val ops = tiles.map { seq => seq.map { tile => tile.map { r => r.convert(newType) } } }
    val builder = new RasterSourceBuilder()

    builder.setRasterDefinition(newDef)
    builder.setOp(ops)

    builder.result
  }

  def min():ValueSource[Int] = 
    self.map(_.findMinMax._1)
        .reduce { (m1,m2) =>
          if(isNoData(m1)) m2
          else if(isNoData(m2)) m1
          else math.min(m1,m2)
         }

  def max():ValueSource[Int] = 
    self.map(_.findMinMax._2)
        .reduce { (m1,m2) =>
          if(isNoData(m1)) m2
          else if(isNoData(m2)) m1
          else math.max(m1,m2)
         }

  def minMax():ValueSource[(Int,Int)] = 
    self.map(_.findMinMax)
        .reduce { (mm1,mm2) =>
          val (min1,max1) = mm1
          val (min2,max2) = mm2
          (if(isNoData(min1)) min2
           else if(isNoData(min2)) min1
           else math.min(min1,min2),
           if(isNoData(max1)) max2
           else if(isNoData(max2)) max1
           else math.max(max1,max2)
          )
         }

  def info:ValueSource[process.RasterLayerInfo] = 
    ValueSource(
      rasterDefinition
        .flatMap { rd => 
          if(rd.catalogued) {
            io.LoadRasterLayerInfo(rd.layerId)
          } else {
            RasterLayerInfo.fromDefinition(rd)
          }
        }
    )

  def rasterExtent:ValueSource[RasterExtent] =
    ValueSource(rasterDefinition.map(_.rasterExtent))

  private def warp(targetOp: Op[RasterExtent]): RasterSource = {

    val newDef: Op[RasterDefinition] =
      (rasterDefinition, targetOp).map { (rd, target) =>
        RasterDefinition(
          rd.layerId,
          target,
          TileLayout.singleTile(target.cols,target.rows),
          rd.rasterType
        )
      }

    val newOp: Op[Seq[Op[Raster]]] =
      (rasterDefinition, tiles, targetOp).flatMap { (rd, seq, target) =>
        if (rd.isTiled) {
          val re = rd.rasterExtent
          val tileLayout = rd.tileLayout

          val targetExtent = target.extent
          val resLayout = tileLayout.getResolutionLayout(re)

          val warped = mutable.ListBuffer[Op[Raster]]()
          val tCols = tileLayout.tileCols
          val tRows = tileLayout.tileRows
          for(tCol <- 0 until tCols optimized) {
            for(tRow <- 0 until tRows optimized) {
              val sourceRasterExtent = resLayout.getRasterExtent(tCol, tRow)
              val sourceExtent = resLayout.getExtent(tCol, tRow)
              sourceExtent.intersect(targetExtent) match {
                case Some(ext) =>
                  val cols = math.ceil((ext.xmax - ext.xmin) / re.cellwidth).toInt
                  val rows = math.ceil((ext.ymax - ext.ymin) / re.cellheight).toInt
                  val tileRe = RasterExtent(ext, re.cellwidth, re.cellheight, cols, rows)

                  // Read section of the tile
                  warped += seq(tCols * tRow + tCol).map(_.warp(tileRe))
                case None => // pass
              }
            }
          }

          if (warped.size == 0) {
            Seq(Literal(Raster(RasterData.emptyByType(rd.rasterType, target.cols, target.rows), target)))
          } else if (warped.size == 1) {
            warped.toSeq
          } else {

            // Create destination raster data
            logic.Collect(warped) map { warped =>
              val data = RasterData.emptyByType(rd.rasterType, re.cols, re.rows)

              for(rasterPart <- warped) {
                val tileRe = rasterPart.rasterExtent

                // Copy over the values to the correct place in the raster data
                val cols = tileRe.cols
                val rows = tileRe.rows

                if (rd.rasterType.isDouble) {
                  for(partCol <- 0 until cols optimized) {
                    for(partRow <- 0 until rows optimized) {
                      val dataCol = re.mapXToGrid(tileRe.gridColToMap(partCol))
                      val dataRow = re.mapYToGrid(tileRe.gridRowToMap(partRow))

                      if (!(dataCol < 0 ||
                            dataCol >= re.cols ||
                            dataRow < 0 || dataRow >= re.rows)
                         ) {
                        data.setDouble(dataCol, dataRow, rasterPart.getDouble(partCol, partRow))
                      }
                    }
                  }
                } else {
                  for(partCol <- 0 until cols optimized) {
                    for(partRow <- 0 until rows optimized) {
                      val dataCol = re.mapXToGrid(tileRe.gridColToMap(partCol))
                      val dataRow = re.mapYToGrid(tileRe.gridRowToMap(partRow))
                      if (!(dataCol < 0 ||
                            dataCol >= re.cols ||
                            dataRow < 0 ||
                            dataRow >= re.rows)
                         ) {
                        data.set(dataCol, dataRow, rasterPart.get(partCol, partRow))
                      }
                    }
                  }
                }
              }
              Seq(Literal(Raster(data, target)))
            }
          }
        } else {
          Seq(seq(0).map(_.warp(target)))
        }
      }

    RasterSource(newDef,newOp)
  }

  def warp(target: RasterExtent): RasterSource =
    warp(Literal(target))

  def warp(target: Extent): RasterSource =
    warp(rasterDefinition.map(_.rasterExtent.createAligned(target)))

  def warp(targetCols: Int, targetRows: Int): RasterSource =
    warp(rasterDefinition.map(_.rasterExtent.withDimensions(targetCols, targetRows)))
}
