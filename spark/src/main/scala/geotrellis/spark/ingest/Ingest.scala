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
import org.apache.spark.storage.StorageLevel
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
   *  - Optionally pyramid to top zoom level, calling sink at each level
   *
   * Ingesting is abstracted over the following variants:
   *  - The source of the input tiles, which are represented as an RDD of (T, Tile) tuples, where T: IngestKey
   *  - The LayoutScheme which will be used to determine how to retile the input tiles.
   *
   * @param sourceTiles   RDD of tiles that have Extent and CRS
   * @param destCRS       CRS to be used by the output layer
   * @param layoutScheme  LayoutScheme to be used by output layer
   * @param pyramid       Pyramid up to level 1, sink function will be called for each level
   * @param tiler         Tiler that can understand the input and out keys (implicit)
   * @param sink          function that utilize the result of the ingest, assumed to force materialization of the RDD
   * @tparam T            type of input tile key
   * @tparam K            type of output tile key, must have SpatialComponent
   * @return
   */
  def apply[T: IngestKey: ClassTag, K: SpatialComponent: ClassTag]
    (sourceTiles: RDD[(T, Tile)], destCRS: CRS, layoutScheme: LayoutScheme, pyramid: Boolean = false)
    (sink: (RasterRDD[K], Int) => Unit)
    (implicit tiler: Tiler[T, K, Tile]): Unit =
  {
    def sinkLevels(rdd: RasterRDD[K], level: Int)(free: => Unit): Unit = {
      if (pyramid && level >= 1) {
        rdd.cache()
        sink(rdd, level)
        free
        val (nextZoom, nextRdd) = Pyramid.up(rdd, layoutScheme, level)
        // we must do it now so we can unpersist the source before recurse
        sinkLevels(nextRdd, nextZoom){ rdd.unpersist(blocking = false) }
      } else {
        sink(rdd, level)
      }
    }

    val reprojectedTiles = sourceTiles.reproject(destCRS).cache()
    // execution is going to fork here to collect the RasterMetaData
    val (layoutLevel, rasterMetaData) =
      RasterMetaData.fromRdd(reprojectedTiles, destCRS, layoutScheme) { key: T =>
        key.projectedExtent.extent
      }

    val tiledRdd = tiler(reprojectedTiles, rasterMetaData).cache()
    val rasterRdd = new RasterRDD(tiledRdd, rasterMetaData)
    sinkLevels(rasterRdd, layoutLevel){ reprojectedTiles.unpersist(blocking = false) }
  }
}
