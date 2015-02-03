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
import geotrellis.raster._
import geotrellis.proj4._
import org.apache.spark.rdd._
import scala.reflect.ClassTag

object Ingest {
  /**
   * Represents the ingest process.
   * An ingest process produces a layer from a set of input rasters.
   *
   * The ingest process has the following steps:
   *
   *  - Reproject tiles to the desired CRS:  (CRS, RDD[(Extent, CRS), Tile)]) -> RDD[(Extent, Tile)]
   *  - Determine the appropriate layer meta data for the layer. (CRS, LayoutScheme, RDD[(Extent, Tile)]) -> LayerMetaData)
   *  - Resample the rasters into the desired tile format. RDD[(Extent, Tile)] => RasterRDD[K]
   *
   * Ingesting is abstracted over the following variants:
   *  - The source of the input tiles, which are represented as an RDD of (T, Tile) tuples, where T: IngestKey
   *  - The LayoutScheme which will be used to determine how to retile the input tiles.
   *
   * Saving and pyramiding can be done by the caller as the result is RasterRDD[K]
   * 
   * @param sourceTiles   RDD of tiles that have Extent and CRS
   * @param destCRS       CRS to be used by the output layer
   * @param LayoutScheme  LayoutScheme to be used by output layer
   * @param isUniform     Flag that all input tiles share an the same extent (optimization)
   * @param tiler         Tiler that can understand the input and out keys (implicit)
   * @tparam T            type of input tile key
   * @tparam K            type of output tile key, must have SpatialComponent
   * @return
   */
  def apply[T: IngestKey: ClassTag, K: SpatialComponent: ClassTag]
    (sourceTiles: RDD[(T, Tile)], destCRS: CRS, layoutScheme: LayoutScheme, isUniform: Boolean = false)
    (implicit tiler: Tiler[T, K]): (LayoutLevel, RasterRDD[K]) =
  {
    val reprojectedTiles = sourceTiles.reproject(destCRS)

    val (layoutLevel, rasterMetaData) =
      RasterMetaData.fromRdd(reprojectedTiles, destCRS, layoutScheme, isUniform) { key: T =>
        key.projectedExtent.extent
      }

    val rasterRdd = tiler(reprojectedTiles, rasterMetaData)

    (layoutLevel, rasterRdd)
  }

  def mosaic(sourceTiles: RDD[(Extent, Tile)], metaData: LayerMetaData): (RDD[TmsTile], LayerMetaData) = {
    val bcMetaData = sc.broadcast(metaData)
    val tiles =
      sourceTiles
        .flatMap { case (extent, tile) =>
          val metaData = bcMetaData.value
          metaData
            .transform
            .mapToIndex(extent)
            .map { tileId =>
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

  def apply(source: =>RDD[(Extent, Tile)], sink:  Sink, sourceCRS: CRS, destCRS: CRS, tilingScheme: TilingScheme): Unit =
    source |>
    reproject(sourceCRS, destCRS) |>
    setMetaData(destCRS, tilingScheme) |>
    mosaic |>
    sink

  // def apply(source: =>RDD[(Extent, Tile)], pyramid: (Sink) => (RDD[TmsTile], LayerMetaData) => Unit, sink:  Sink): Unit =
  //   source |>
  //   setMetaData |>
  //   mosaic |>
  //   pyramid(sink)
}
