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

package geotrellis.raster

import geotrellis.feature._
import geotrellis.raster.op._
import geotrellis.raster.stats._
import geotrellis.raster.render.op._
import geotrellis.raster.io._
import geotrellis.engine._

import spire.syntax.cfor._
import scala.collection.mutable

trait RasterSourceLike[+Repr <: RasterSource] 
    extends DataSourceLike[Tile, Tile, Repr]
    with DataSource[Tile, Tile] 
    with local.LocalOpMethods[Repr] 
    with focal.FocalOpMethods[Repr]
    with global.GlobalOpMethods[Repr]
    with zonal.ZonalOpMethods[Repr]
    with zonal.summary.ZonalSummaryOpMethods[Repr]
    with hydrology.HydrologyOpMethods[Repr]
    with stats.StatOpMethods[Repr] 
    with RenderOpMethods[Repr] { self: Repr =>

  def tiles = self.elements
  def rasterDefinition: Op[RasterDefinition]

  def convergeOp(): Op[Tile] =
    tiles.flatMap { ts =>
      if(ts.size == 1) { ts(0) }
      else { 
        (rasterDefinition, logic.Collect(ts)).map { (rd, tileSeq) =>
          CompositeTile(tileSeq, rd.tileLayout).toArrayTile
        }
      }
    }

  def global[That](f: Tile=>Tile)
                  (implicit bf: CanBuildSourceFrom[Repr, Tile, That]): That = {
    val tileOps: Op[Seq[Op[Tile]]] =
      (rasterDefinition, logic.Collect(tiles)).map { (rd, tileSeq) =>
        if(rd.isTiled) {
          val r = f(CompositeTile(tileSeq.toSeq, rd.tileLayout))
          CompositeTile.split(r, rd.tileLayout).map(Literal(_))
        } else {
          Seq(f(tileSeq(0)))
        }
      }
    // Set into new RasterSource
    val builder = bf.apply(this)
    builder.setOp(tileOps)
    builder.result
  }

  def globalOp[T, That](f: Tile=>Op[Tile])
                      (implicit bf: CanBuildSourceFrom[Repr, Tile, That]): That = {
    val tileOps: Op[Seq[Op[Tile]]] =
      (rasterDefinition, logic.Collect(tiles)).flatMap { (rd, tileSeq) =>
        if(rd.isTiled) {
          f(CompositeTile(tileSeq.toSeq, rd.tileLayout)).map { r =>
            CompositeTile.split(r, rd.tileLayout).map(Literal(_))
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

  def convert(newType: CellType) = {
    val newDef = rasterDefinition.map(_.withType(newType))
    val ops = tiles.map { seq => seq.map { tile => tile.map { r => r.convert(newType) } } }
    val builder = new RasterSourceBuilder()

    builder.setRasterDefinition(newDef)
    builder.setOp(ops)

    builder.result
  }

  def min(): ValueSource[Int] = 
    self.map(_.findMinMax._1)
        .reduce { (m1, m2) =>
          if(isNoData(m1)) m2
          else if(isNoData(m2)) m1
          else math.min(m1, m2)
         }

  def max(): ValueSource[Int] = 
    self.map(_.findMinMax._2)
        .reduce { (m1, m2) =>
          if(isNoData(m1)) m2
          else if(isNoData(m2)) m1
          else math.max(m1, m2)
         }

  def minMax(): ValueSource[(Int, Int)] = 
    self.map(_.findMinMax)
        .reduce { (mm1, mm2) =>
          val (min1, max1) = mm1
          val (min2, max2) = mm2
          (if(isNoData(min1)) min2
           else if(isNoData(min2)) min1
           else math.min(min1, min2), 
           if(isNoData(max1)) max2
           else if(isNoData(max2)) max1
           else math.max(max1, max2)
          )
         }

  def info: ValueSource[RasterLayerInfo] = 
    ValueSource(
      rasterDefinition
        .flatMap { rd => 
          if(rd.catalogued) {
            LoadRasterLayerInfo(rd.layerId)
          } else {
            RasterLayerInfo.fromDefinition(rd)
          }
        }
    )

  def rasterExtent: ValueSource[RasterExtent] =
    ValueSource(rasterDefinition.map(_.rasterExtent))

  private def warp(targetOp: Op[RasterExtent]): RasterSource = {

    val newDef: Op[RasterDefinition] =
      (rasterDefinition, targetOp).map { (rd, target) =>
        RasterDefinition(
          rd.layerId,
          target,
          TileLayout.singleTile(target.cols, target.rows),
          rd.cellType
        )
      }

    val newOp: Op[Seq[Op[Tile]]] =
      (rasterDefinition, tiles, targetOp).flatMap { (rd, seq, target) =>
        if (rd.isTiled) {
          val re = rd.rasterExtent
          val tileLayout = rd.tileLayout

          val targetExtent = target.extent
          val resLayout = tileLayout.getResolutionLayout(re)

          val warped = mutable.ListBuffer[Op[(Tile, Extent)]]()
          val tCols = tileLayout.tileCols
          val tRows = tileLayout.tileRows
          cfor(0)(_ < tCols, _ + 1) { tCol =>
            cfor(0)(_ < tRows, _ + 1) { tRow =>
              val sourceExtent = resLayout.getExtent(tCol, tRow)
              sourceExtent.intersection(targetExtent) match {
                case Some(ext) =>
                  val cols = math.ceil((ext.xmax - ext.xmin) / re.cellwidth).toInt
                  val rows = math.ceil((ext.ymax - ext.ymin) / re.cellheight).toInt
                  val tileRe = RasterExtent(ext, re.cellwidth, re.cellheight, cols, rows)

                  // Read section of the tile
                  warped += seq(tCols * tRow + tCol).map { r => (r.warp(sourceExtent, tileRe), ext) }
                case None => // pass
              }
            }
          }

          if (warped.size == 0) {
            Seq(Literal(ArrayTile.empty(rd.cellType, target.cols, target.rows)))
          } else if (warped.size == 1) {
            Seq(warped.head.map(_._1))
          } else {

            // Create destination raster data
            logic.Collect(warped) map { warped =>
              val tile = ArrayTile.empty(rd.cellType, re.cols, re.rows)

              for((rasterPart, extent) <- warped) {
                // Copy over the values to the correct place in the raster data
                val cols = rasterPart.cols
                val rows = rasterPart.rows
                val tileRe = RasterExtent(extent, cols, rows)

                if (rd.cellType.isFloatingPoint) {
                  cfor(0)(_ < rows, _ + 1) { partRow =>
                    cfor(0)(_ < cols, _ + 1) { partCol =>
                      val tileCol = re.mapXToGrid(tileRe.gridColToMap(partCol))
                      val tileRow = re.mapYToGrid(tileRe.gridRowToMap(partRow))

                      if (!(tileCol < 0 ||
                            tileCol >= re.cols ||
                            tileRow < 0 || tileRow >= re.rows)
                         ) {
                        tile.setDouble(tileCol, tileRow, rasterPart.getDouble(partCol, partRow))
                      }
                    }
                  }
                } else {
                  cfor(0)(_ < rows, _ + 1) { partRow =>
                    cfor(0)(_ < cols, _ + 1) { partCol =>
                      val tileCol = re.mapXToGrid(tileRe.gridColToMap(partCol))
                      val tileRow = re.mapYToGrid(tileRe.gridRowToMap(partRow))
                      if (!(tileCol < 0 ||
                            tileCol >= re.cols ||
                            tileRow < 0 ||
                            tileRow >= re.rows)
                         ) {
                        tile.set(tileCol, tileRow, rasterPart.get(partCol, partRow))
                      }
                    }
                  }
                }
              }
              Seq(Literal(tile))
            }
          }
        } else {
          Seq(seq(0).map(_.warp(rd.rasterExtent.extent, target)))
        }
      }

    RasterSource(newDef, newOp)
  }

  def warp(target: RasterExtent): RasterSource =
    warp(Literal(target))

  def warp(target: Extent): RasterSource =
    warp(rasterDefinition.map(_.rasterExtent.createAligned(target)))

  def warp(targetCols: Int, targetRows: Int): RasterSource =
    warp(rasterDefinition.map(_.rasterExtent.withDimensions(targetCols, targetRows)))
}
