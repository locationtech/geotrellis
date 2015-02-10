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

package geotrellis.engine

import geotrellis.engine.op._
import geotrellis.engine.io._
import geotrellis.raster._
import geotrellis.raster.stats._

import geotrellis.vector.Extent

import akka.actor.ActorRef

import spire.syntax.cfor._
import scala.collection.mutable

class RasterSource(val rasterDef: Op[RasterDefinition], val tileOps: Op[Seq[Op[Tile]]])
    extends DataSource[Tile, Tile] {
  type Self = RasterSource

  def elements = tileOps
  val rasterDefinition = rasterDef

  def tiles = elements
  def rasters: Op[Seq[Op[Raster]]] =
    (rasterDefinition, tiles).map { (rd, seq) =>
      val tileExtents = rd.tileExtents
      seq.zipWithIndex.map { case (tileOp, tileIndex) =>
        tileOp map { tile => Raster(tile, tileExtents(tileIndex)) }
      }
    }

  def withElements(newElements: Op[Seq[Op[Tile]]]): DataSource[Tile, Tile] =
    RasterSource(rasterDefinition, newElements)

  def convergeOp(): Op[Tile] =
    tiles.flatMap { ts =>
      if(ts.size == 1) { ts(0) }
      else {
        (rasterDefinition, logic.Collect(ts)).map { (rd, tileSeq) =>
          CompositeTile(tileSeq, rd.tileLayout).toArrayTile
        }
      }
    }

  def global[That](f: Tile=>Tile): RasterSource = {
    val tileOps: Op[Seq[Op[Tile]]] =
      (rasterDefinition, logic.Collect(tiles)).map { (rd, tileSeq) =>
        if(rd.isTiled) {
          val r = f(CompositeTile(tileSeq.toSeq, rd.tileLayout))
          CompositeTile.split(r, rd.tileLayout).map(Literal(_))
        } else {
          Seq(f(tileSeq(0)))
        }
      }
    RasterSource(rasterDefinition, tileOps)
  }

  def globalOp[T, That](f: Tile=>Op[Tile]): RasterSource = {
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
    RasterSource(rasterDefinition, tileOps)
  }

  /** apply a function to elements, and return the appropriate datasource **/
  def mapTile(f: Tile => Tile): RasterSource =
    mapTileOp(Op(f(_)))

  /** apply a function to elements, and return the appropriate datasource **/
  def mapTile(f: Tile => Tile, name: String): RasterSource =
    mapTileOp(Op(f(_)), name)

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapTileOp(f: Op[Tile] => Op[Tile]): RasterSource =
    mapTileOp(f, s"${getClass.getSimpleName} map")

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapTileOp(f: Op[Tile] => Op[Tile], name: String): RasterSource = {
    val newOp = elements.map(_.map(f)).withName(name)
    RasterSource(rasterDefinition, newOp)
  }

  /** apply a function to elements, and return the appropriate datasource **/
  def mapRaster(f: Raster => Tile): RasterSource =
    mapRasterOp { op => op.map(f(_)) }

  /** apply a function to elements, and return the appropriate datasource **/
  def mapRaster(f: Raster => Tile, name: String): RasterSource =
    mapRasterOp({ op => op.map(f(_)) }, name)

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapRasterOp(f: Op[Raster] => Op[Tile]): RasterSource =
    mapRasterOp(f, s"${getClass.getSimpleName} map")

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapRasterOp(f: Op[Raster] => Op[Tile], name: String): RasterSource = {
    val newOp = rasters.map(_.map(f)).withName(name)
    RasterSource(rasterDefinition, newOp)
  }

  /** apply a function to elements, and return the appropriate datasource **/
  def mapWithExtent[T](f: (Tile, Extent) => T)(implicit d: DummyImplicit): SeqSource[T] =
    mapWithExtentOp { op => op.map { case (t, e) => f(t, e) } }

  /** apply a function to elements, and return the appropriate datasource **/
  def mapWithExtent[T](f: (Tile, Extent) => T, name: String)(implicit d: DummyImplicit): SeqSource[T] =
    mapWithExtentOp({ op => op.map { case (t, e) => f(t, e) } }, name)

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapWithExtentOp[T](f: Op[(Tile, Extent)] => Op[T])(implicit d: DummyImplicit): SeqSource[T] =
    mapWithExtentOp(f, s"${getClass.getSimpleName} map")

  /** apply a function to element operations, and return the appropriate datasource **/
  def mapWithExtentOp[T](f: Op[(Tile, Extent)] => Op[T], name: String)(implicit d: DummyImplicit): SeqSource[T] = {
    val newOp = rasters.map(_.map { r => r.flatMap { case Raster(t, e) => f(t, e) } }).withName(name)
    SeqSource(newOp)
  }

  def combineTile(rs: RasterSource)(f: (Tile, Tile)=> Tile) =
    combineTileOp(rs)( { (a, b) => (a, b).map(f(_, _)) })

  def combineTile(rs: RasterSource, name: String)(f: (Tile, Tile)=> Tile) =
    combineTileOp(rs, name)( { (a, b) => (a, b).map(f(_, _)) })

  def combineTile(rss: Seq[RasterSource])(f: Seq[Tile] => Tile): RasterSource =
     combineTileOp(rss)(_.mapOps(f(_)))

  def combineTile(rss: Seq[RasterSource], name: String)(f: Seq[Tile] => Tile): RasterSource =
     combineTileOp(rss, name)(_.mapOps(f(_)))

  def combineTileOp(rs: RasterSource)(f: (Op[Tile], Op[Tile])=>Op[Tile]): RasterSource =
    combineTileOp(rs, s"${getClass.getSimpleName} combine")(f)

  def combineTileOp(rs: RasterSource, name: String)(f: (Op[Tile], Op[Tile])=>Op[Tile]): RasterSource = {
    val newElements: Op[Seq[Op[Tile]]] =
      ((elements, rs.elements).map { (e1, e2) =>
        e1.zip(e2).map { case (a, b) =>
          f(a, b)
        }
      }).withName(name)
    RasterSource(rasterDefinition, newElements)
  }

  def combineTileOp(rss: Seq[RasterSource])(f: Seq[Op[Tile]]=>Op[Tile]): RasterSource =
    combineTileOp(rss, s"${getClass.getSimpleName} combine")(f)

  def combineTileOp(rss: Seq[RasterSource], name: String)(f: Seq[Op[Tile]]=>Op[Tile]): RasterSource = {
    val newElements: Op[Seq[Op[Tile]]] =
      (elements +: rss.map(_.elements)).mapOps { seq =>
        seq.transpose.map(f)
      }.withName(name)

    RasterSource(rasterDefinition, newElements)
  }

  def convert(newType: CellType) = {
    val newDef = rasterDefinition.map(_.withType(newType))
    val ops = tiles.map { seq => seq.map { tile => tile.map { r => r.convert(newType) } } }

    RasterSource(newDef, ops)
  }

  def min(): ValueSource[Int] =
    map(_.findMinMax._1)
      .reduce { (m1, m2) =>
        if(isNoData(m1)) m2
        else if(isNoData(m2)) m1
        else math.min(m1, m2)
       }

  def max(): ValueSource[Int] =
    map(_.findMinMax._2)
      .reduce { (m1, m2) =>
        if(isNoData(m1)) m2
        else if(isNoData(m2)) m1
        else math.max(m1, m2)
       }

  def minMax(): ValueSource[(Int, Int)] =
    map(_.findMinMax)
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

  private def resample(targetOp: Op[RasterExtent]): RasterSource = {

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
          val tileExtents = TileExtents(re.extent, tileLayout)

          val resampled = mutable.ListBuffer[Op[Raster]]()
          val tCols = tileLayout.layoutCols
          val tRows = tileLayout.layoutRows
          cfor(0)(_ < tRows, _ + 1) { tRow =>
            cfor(0)(_ < tCols, _ + 1) { tCol =>
              val sourceExtent = tileExtents(tCol, tRow)
              sourceExtent.intersection(targetExtent) match {
                case Some(ext) =>
                  val cols = math.ceil((ext.xmax - ext.xmin) / re.cellwidth).toInt
                  val rows = math.ceil((ext.ymax - ext.ymin) / re.cellheight).toInt
                  val tileRe = RasterExtent(ext, re.cellwidth, re.cellheight, cols, rows)

                  // Read section of the tile
                  resampled += seq(tCols * tRow + tCol).map { r => (r.resample(sourceExtent, tileRe), ext) }
                case None => // pass
              }
            }
          }

          if (resampled.size == 0) {
            Seq(Literal(ArrayTile.empty(rd.cellType, target.cols, target.rows)))
          } else if (resampled.size == 1) {
            Seq(resampled.head.map(_.tile))
          } else {

            // Create destination raster data
            logic.Collect(resampled) map { resampled =>
              val tile = ArrayTile.empty(rd.cellType, target.cols, target.rows)

              for(Raster(rasterPart, extent) <- resampled) {
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
          Seq(seq(0).map(_.resample(rd.rasterExtent.extent, target)))
        }
      }

    RasterSource(newDef, newOp)
  }

  def resample(target: RasterExtent): RasterSource =
    resample(Literal(target))

  def resample(target: Extent): RasterSource =
    resample(rasterDefinition.map(_.rasterExtent.createAligned(target)))

  def resample(targetCols: Int, targetRows: Int): RasterSource =
    resample(rasterDefinition.map(_.rasterExtent.withDimensions(targetCols, targetRows)))

  def distribute(cluster: Option[ActorRef]) = RasterSource(rasterDefinition, distributeOps(cluster))
  def cached(implicit engine: Engine) = RasterSource(rasterDefinition, cachedOps(engine))

}

object RasterSource {
  def fromPath(path: String): RasterSource =
    fromPath(path, None)

  def fromPath(path: String, rasterExtent: RasterExtent): RasterSource =
    fromPath(path, Some(rasterExtent))

  def fromPath(path: String, targetExtent: Option[RasterExtent]): RasterSource =
    apply(LoadRasterLayerFromPath(path), targetExtent)

  def fromUrl(jsonUrl: String): RasterSource =
    fromUrl(jsonUrl, None)

  def fromUrl(jsonUrl: String, targetExtent: RasterExtent): RasterSource =
    fromUrl(jsonUrl, Some(targetExtent))

  def fromUrl(jsonUrl: String, targetExtent: Option[RasterExtent]): RasterSource =
    apply(LoadRasterLayerFromUrl(jsonUrl), targetExtent)

  def apply(rasterLayer: Op[RasterLayer], targetExtent: Option[RasterExtent])
           (implicit d: DI): RasterSource = {
    val rasterDef =
      rasterLayer map { layer =>
        RasterDefinition(layer.info.id,
                         layer.info.rasterExtent,
                         layer.info.tileLayout,
                         layer.info.cellType)
      }

    val tileOps =
      targetExtent match {
        case re @ Some(_) =>
          rasterLayer.map { layer =>
            Seq(Literal(layer.getRaster(re)))
          }
        case None =>
          rasterLayer.map { layer =>
            (for(tileRow <- 0 until layer.info.tileLayout.layoutRows;
              tileCol <- 0 until layer.info.tileLayout.layoutCols) yield {
              Literal(layer.getTile(tileCol, tileRow))
            })
          }
      }

    RasterSource(rasterDef, tileOps)
  }

  def apply(name: String): RasterSource =
    RasterSource(LoadRasterDefinition(LayerId(name)), None)

  def apply(name: String, rasterExtent: RasterExtent): RasterSource =
    RasterSource(LoadRasterDefinition(LayerId(name)), Some(rasterExtent))

  def apply(store: String, name: String): RasterSource =
    RasterSource(LoadRasterDefinition(LayerId(store, name)), None)

  def apply(store: String, name: String, rasterExtent: RasterExtent): RasterSource =
    RasterSource(LoadRasterDefinition(LayerId(store, name)), Some(rasterExtent))

  def apply(layerId: LayerId): RasterSource =
    RasterSource(LoadRasterDefinition(layerId), None)

  def apply(layerId: LayerId, rasterExtent: RasterExtent): RasterSource =
    RasterSource(LoadRasterDefinition(layerId), Some(rasterExtent))

  def apply(rasterDef: Op[RasterDefinition]): RasterSource =
    apply(rasterDef, None)

  def apply(rasterDef: Op[RasterDefinition], targetExtent: RasterExtent): RasterSource =
    apply(rasterDef, Some(targetExtent))

  def apply(rasterDef: Op[RasterDefinition], targetExtent: Option[RasterExtent]): RasterSource = {
    val (rd, tileOps) =
      targetExtent match {
        case reOp @ Some(re) =>
          ( rasterDef.map(_.withRasterExtent(re)),
            rasterDef.map { rd =>
              Seq(LoadRaster(rd.layerId, reOp))
            }
          )
        case None =>
          ( rasterDef,
            rasterDef.map { rd =>
              (for(tileRow <- 0 until rd.tileLayout.layoutRows;
                tileCol <- 0 until rd.tileLayout.layoutCols) yield {
                LoadTile(rd.layerId, tileCol, tileRow)
              })
            }
          )
      }

    new RasterSource(rd, tileOps)
  }

  def apply(rasterDef: Op[RasterDefinition], tileOps: Op[Seq[Op[Tile]]]) =
    new RasterSource(rasterDef, tileOps)

  /** Create a RasterSource who's tile ops are the tiles of a CompositeTile. */
  def apply(compositeTile: CompositeTile, extent: Extent): RasterSource = {
    val rasterDef =
      RasterDefinition(
        LayerId("LiteralCompositeTile"),
        RasterExtent(extent, compositeTile.cols, compositeTile.rows),
        compositeTile.tileLayout,
        compositeTile.cellType
      )

    val tileOps = compositeTile.tiles.map(Literal(_))

    new RasterSource(rasterDef, tileOps)
  }

  def apply(compositeTile: Op[CompositeTile], extent: Op[Extent])(implicit d: DI): RasterSource = {
    val rasterDef = (compositeTile, extent).map { (tr, ext) =>
      RasterDefinition(
        LayerId("LiteralCompositeTile"),
        RasterExtent(ext, tr.cols, tr.rows),
        tr.tileLayout,
        tr.cellType
      )
    }
    val tileOps = compositeTile.map(_.tiles.map(Literal(_)))
    new RasterSource(rasterDef, tileOps)
  }

  def apply(raster: Op[Tile], extent: Op[Extent])(implicit d: DI, d2: DI): RasterSource = {
    val rasterDef =
      (raster, extent).map { (r, ext) =>
        RasterDefinition(LayerId("LiteralRaster"),
          RasterExtent(ext, r.cols, r.rows),
          TileLayout.singleTile(r.cols, r.rows),
          r.cellType
        )
      }

    val tileOps = Literal(Seq(raster))
    new RasterSource(rasterDef, tileOps)
  }

  def apply(r: Tile, extent: Extent): RasterSource = {
    val rasterDef =
      Literal {
        RasterDefinition(LayerId("LiteralRaster"),
          RasterExtent(extent, r.cols, r.rows),
          TileLayout.singleTile(r.cols, r.rows),
          r.cellType
        )
      }

    val tileOps = Literal(Seq(Literal(r)))
    new RasterSource(rasterDef, tileOps)
  }

  def layerExists(layerId: LayerId): Boolean = engine.layerExists(layerId)
  def layerExists(layerName: String): Boolean = engine.layerExists(layerName)
}
