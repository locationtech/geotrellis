/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.ingest

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.proj4._

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import org.apache.spark.broadcast.Broadcast

import spire.syntax.cfor._

object Ingest { def apply(sc: SparkContext): Ingest = new Ingest(sc) }

class Ingest(sc: SparkContext) {
  type Sink = (RDD[TmsTile], LayerMetaData) => Unit

  def reproject(destCRS: CRS): RDD[((Extent, CRS), Tile)] => RDD[((Extent, CRS), Tile)] = {
    def _reproject(sourceTiles: RDD[((Extent, CRS), Tile)]): RDD[((Extent, CRS), Tile)] = {
      sourceTiles.map { case ((extent, crs), tile) => {
        if (crs == destCRS) ((extent, crs), tile)
        else {
          val (t, e) = tile.reproject(extent, crs, destCRS)
          ((e, destCRS), t)
        }
      }
      }
    }
    _reproject
  }

  def setMetaData(tilingScheme: TilingScheme): RDD[((Extent, CRS), Tile)] => (RDD[((Extent, CRS), Tile)], LayerMetaData) = {
    def _setMetaData(sourceTiles: RDD[((Extent, CRS), Tile)]): (RDD[((Extent, CRS), Tile)], LayerMetaData) =  {

      val (uncappedExtent, cellType, cellSize, crs): (Extent, CellType, CellSize, CRS) =
        sourceTiles
          .map { case ((extent, crs), tile) => (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows), crs) }
          .reduce { (t1, t2) =>
          val (e1, ct1, cs1, crs1) = t1
          val (e2, ct2, cs2, crs2) = t2
          (
            e1.combine(e2),
            ct1.union(ct2),
            if(cs1.resolution < cs2.resolution) cs1 else cs2,
            crs1
          )
        }

      // TODO: Allow variance of TilingScheme and TileIndexScheme
      val tileIndexScheme: TileIndexScheme = RowIndexScheme

      val worldExtent = crs.worldExtent
      val layerLevel: LayoutLevel = tilingScheme.layoutFor(worldExtent, cellSize)

      val extentIntersection = worldExtent.intersection(uncappedExtent).get

      val metaData =
        LayerMetaData(cellType, extentIntersection, crs, layerLevel, tileIndexScheme)

      (sourceTiles, metaData)
    }

    _setMetaData
  }

  def mosaic(sourceTiles: RDD[((Extent, CRS), Tile)], metaData: LayerMetaData): (RDD[TmsTile], LayerMetaData) = {
    val bcMetaData = sc.broadcast(metaData)
    val tiles =
      sourceTiles
        .flatMap { case ((extent, _), tile) =>
          val metaData = bcMetaData.value
          metaData
            .transform
            .mapToGrid(extent)
            .coords
            .map { coord =>
            val tileId = metaData.transform.gridToIndex(coord)
            (tileId, (tileId, extent, tile))
          }
      }
        .combineByKey(
        { case (tileId, extent, tile) =>
          val metaData = bcMetaData.value
          val tmsTile = ArrayTile.empty(metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
          tmsTile.merge(metaData.transform.indexToMap(tileId), extent, tile)
        },
          { (tmsTile: MutableArrayTile, tup: (Long, Extent, Tile)) =>
            val metaData = bcMetaData.value
            val (tileId, extent, tile) = tup
            tmsTile.merge(metaData.transform.indexToMap(tileId), extent, tile)
          },
          { (tmsTile1: MutableArrayTile , tmsTile2: MutableArrayTile) =>
            tmsTile1.merge(tmsTile2)
          }
      )
        .map { case (id, tile) => TmsTile(id, tile) }
    (tiles, metaData)
  }

  def apply(source: =>RDD[((Extent, CRS), Tile)], sink:  Sink, destCRS: CRS, tilingScheme: TilingScheme): Unit =
    source |>
  reproject(destCRS) |>
  setMetaData(tilingScheme) |>
  mosaic |>
  sink

  // def apply(source: =>RDD[(Extent, Tile)], pyramid: (Sink) => (RDD[TmsTile], LayerMetaData) => Unit, sink:  Sink): Unit =
  //   source |>
  //   setMetaData |>
  //   mosaic |>
  //   pyramid(sink)
}
