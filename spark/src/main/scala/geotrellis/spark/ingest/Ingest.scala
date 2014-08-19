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
import geotrellis.vector._

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
  type Sink = (RDD[TmsTile], LayerMetaData)=>Unit

  def setMetaData(sourceTiles: RDD[(Extent, Tile)]): (RDD[(Extent, Tile)], LayerMetaData) =  {
    val (uncappedExtent, cellType, cellSize): (Extent, CellType, CellSize) =
      sourceTiles
        .map { case (extent, tile) => (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows)) }
        .reduce { (t1, t2) =>
        val (e1, ct1, cs1) = t1
        val (e2, ct2, cs2) = t2
        (e1.combine(e2), ct1.union(ct2),
          if(cs1.resolution < cs2.resolution) cs1 else cs2
        )
      }

    val tileScheme: TilingScheme = TilingScheme.GEODETIC
    val zoomLevel: ZoomLevel = tileScheme.zoomLevelFor(cellSize)

    val extent = tileScheme.extent.intersection(uncappedExtent).get
    (sourceTiles, LayerMetaData(cellType, extent, zoomLevel))
  }

  def mosaic(sourceTiles: RDD[(Extent, Tile)], metaData: LayerMetaData): (RDD[TmsTile], LayerMetaData) = {
    val bcMetaData = sc.broadcast(metaData)
    val tiles =
      sourceTiles
        .flatMap { case (extent, tile) =>
          val zoomLevel = bcMetaData.value.zoomLevel
          zoomLevel.tileIdsForExtent(extent).map { case tileId  => (tileId, (tileId, extent, tile)) }
         }
        .combineByKey( 
          { case (tileId, extent, tile) =>
            val LayerMetaData(cellType, _, zoomLevel) = bcMetaData.value
            val tmsTile = ArrayTile.empty(cellType, zoomLevel.pixelCols, zoomLevel.pixelRows)
            tmsTile.merge(zoomLevel.extentForTile(tileId), extent, tile)
          },
          { (tmsTile: MutableArrayTile, tup: (Long, Extent, Tile)) =>
            val zoomLevel = bcMetaData.value.zoomLevel
            val (tileId, extent, tile) = tup
            tmsTile.merge(zoomLevel.extentForTile(tileId), extent, tile)
          },
          { (tmsTile1: MutableArrayTile , tmsTile2: MutableArrayTile) =>
            tmsTile1.merge(tmsTile2)
          }
         )
        .map { case (id, tile) => TmsTile(id, tile) }
    (tiles, metaData)
  }

  def apply(source: =>RDD[(Extent, Tile)], sink:  Sink): Unit =
    source |>
    setMetaData |>
    mosaic |>
    sink

  //   apply(source, { (rdd, meta) => ObservableCollection((rdd, meta)) }, sink)

  // def apply(source: =>RDD[(Extent, Tile)], pyramid: (Sink) => (RDD[TmsTile], LayerMetaData) => Unit, sink:  Sink): Unit =
  //   source |>
  //   setMetaData |>
  //   mosaic |>
  //   pyramid(sink)
}
