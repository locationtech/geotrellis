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

package geotrellis.source

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.process.{LayerId, RasterLayer}
import geotrellis.raster.op._
import geotrellis.raster.statistics.Histogram
import geotrellis.raster._

class RasterSource(val rasterDef: Op[RasterDefinition], val tileOps: Op[Seq[Op[Tile]]]) 
    extends  RasterSourceLike[RasterSource] {
  def elements = tileOps
  val rasterDefinition = rasterDef

}

object RasterSource {
  def fromPath(path: String): RasterSource = 
    fromPath(path, None)

  def fromPath(path: String, rasterExtent: RasterExtent): RasterSource =
    fromPath(path, Some(rasterExtent))

  def fromPath(path: String, targetExtent: Option[RasterExtent]): RasterSource =
    apply(io.LoadRasterLayerFromPath(path), targetExtent)

  def fromUrl(jsonUrl: String): RasterSource =
    fromUrl(jsonUrl, None)

  def fromUrl(jsonUrl: String, targetExtent: RasterExtent): RasterSource =
    fromUrl(jsonUrl, Some(targetExtent))

  def fromUrl(jsonUrl: String, targetExtent: Option[RasterExtent]): RasterSource =
    apply(io.LoadRasterLayerFromUrl(jsonUrl), targetExtent)

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
            (for(tileRow <- 0 until layer.info.tileLayout.tileRows;
              tileCol <- 0 until layer.info.tileLayout.tileCols) yield {
              Literal(layer.getTile(tileCol, tileRow))
            })
          }
      }

    RasterSource(rasterDef, tileOps)
  }

  def apply(name: String): RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(name)), None)

  def apply(name: String, rasterExtent: RasterExtent): RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(name)), Some(rasterExtent))

  def apply(store: String, name: String): RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(store, name)), None)

  def apply(store: String, name: String, rasterExtent: RasterExtent): RasterSource =
    RasterSource(io.LoadRasterDefinition(LayerId(store, name)), Some(rasterExtent))

  def apply(layerId: LayerId): RasterSource =
    RasterSource(io.LoadRasterDefinition(layerId), None)

  def apply(layerId: LayerId, rasterExtent: RasterExtent): RasterSource =
    RasterSource(io.LoadRasterDefinition(layerId), Some(rasterExtent))

  def apply(rasterDef: Op[RasterDefinition]): RasterSource = 
    apply(rasterDef, None)

  def apply(rasterDef: Op[RasterDefinition], targetExtent: RasterExtent): RasterSource = 
    apply(rasterDef, Some(targetExtent))

  def apply(rasterDef: Op[RasterDefinition], targetExtent: Option[RasterExtent]): RasterSource = {
    val tileOps = 
      targetExtent match {
        case re @ Some(_) =>
          rasterDef.map { rd =>
            Seq(io.LoadRaster(rd.layerId, re))
          }
        case None =>
          rasterDef.map { rd =>
            (for(tileRow <- 0 until rd.tileLayout.tileRows;
              tileCol <- 0 until rd.tileLayout.tileCols) yield {
              io.LoadTile(rd.layerId, tileCol, tileRow)
            })
          }
      }

    new RasterSource(rasterDef, tileOps)
  }

  def apply(rasterDef: Op[RasterDefinition], tileOps: Op[Seq[Op[Tile]]]) =
    new RasterSource(rasterDef, tileOps)

  /** Create a RasterSource who's tile ops are the tiles of a TileRaster. */
  def apply(tiledRaster: TileRaster, extent: Extent): RasterSource = {
    val rasterDef = 
      RasterDefinition(
        LayerId("LiteralTileRaster"),
        RasterExtent(extent, tiledRaster.cols, tiledRaster.rows),
        tiledRaster.tileLayout,
        tiledRaster.cellType
      )
    
    val tileOps = tiledRaster.tiles.map(Literal(_))

    new RasterSource(rasterDef, tileOps)
  }

  def apply(tiledRaster: Op[CompositeTile], extent: Op[Extent])(implicit d: DI): RasterSource = {
    val rasterDef = (tiledRaster, extent).map { (tr, ext) =>
      RasterDefinition(
        LayerId("LiteralTileRaster"),
        RasterExtent(ext, tr.cols, tr.rows),
        tr.tileLayout,
        tr.cellType
      )
    }
    val tileOps = tiledRaster.map(_.tiles.map(Literal(_)))
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

  def layerExists(layerId: LayerId): Boolean = server.layerExists(layerId)
  def layerExists(layerName: String): Boolean = server.layerExists(layerName)
}
