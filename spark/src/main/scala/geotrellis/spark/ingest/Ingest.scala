/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.ingest

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample.{ResampleMethod, NearestNeighbor}
import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.spark._
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.reproject._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.Partitioner
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
   *  - Determine the appropriate layer meta data for the layer. (CRS, LayoutScheme, RDD[(Extent, Tile)]) -> LayerMetadata)
   *  - Resample the rasters into the desired tile format. RDD[(Extent, Tile)] => TileLayerRDD[K]
   *  - Optionally pyramid to top zoom level, calling sink at each level
   *
   * Ingesting is abstracted over the following variants:
   *  - The source of the input tiles, which are represented as an RDD of (T, Tile) tuples, where T: Component[?, ProjectedExtent]
   *  - The LayoutScheme which will be used to determine how to retile the input tiles.
   *
   * @param sourceTiles   RDD of tiles that have Extent and CRS
   * @param destCRS       CRS to be used by the output layer
   * @param layoutScheme  LayoutScheme to be used by output layer
   * @param pyramid       Pyramid up to level 1, sink function will be called for each level
   * @param cacheLevel    Storage level to use for RDD caching
   * @param sink          function that utilize the result of the ingest, assumed to force materialization of the RDD
   * @tparam T            type of input tile key
   * @tparam K            type of output tile key, must have SpatialComponent
   * @return
   */
  def apply[T: ClassTag: ? => TilerKeyMethods[T, K]: Component[?, ProjectedExtent], K: SpatialComponent: Boundable: ClassTag](
      sourceTiles: RDD[(T, Tile)],
      destCRS: CRS,
      layoutScheme: LayoutScheme,
      pyramid: Boolean = false,
      cacheLevel: StorageLevel = StorageLevel.NONE,
      resampleMethod: ResampleMethod = NearestNeighbor,
      partitioner: Option[Partitioner] = None,
      bufferSize: Option[Int] = None,
      maxZoom: Option[Int] = None,
      tileSize: Option[Int] = None)
    (sink: (TileLayerRDD[K], Int) => Unit): Unit = {

    val (_, tileLayerMetadata) = tileSize match {
      case Some(ts) => sourceTiles.collectMetadata(FloatingLayoutScheme(ts))
      case _ => sourceTiles.collectMetadata(FloatingLayoutScheme(256))
    }

    val contextRdd = sourceTiles.tileToLayout(tileLayerMetadata, resampleMethod).cache()

    val (zoom, tileLayerRdd) = (layoutScheme, maxZoom) match {
      case (layoutScheme: ZoomedLayoutScheme, Some(mz)) =>
        val LayoutLevel(zoom, layoutDefinition) = layoutScheme.levelForZoom(mz)
        (zoom, bufferSize match {
          case Some(bs) => contextRdd.reproject(destCRS, layoutDefinition, bs, options = RasterReprojectOptions(method = resampleMethod, targetCellSize = Some(layoutDefinition.cellSize)))._2
          case _ => contextRdd.reproject(destCRS, layoutDefinition, options = RasterReprojectOptions(method = resampleMethod, targetCellSize = Some(layoutDefinition.cellSize)))._2
        })

      case _ => bufferSize match {
        case Some(bs) => contextRdd.reproject(destCRS, layoutScheme, bs, resampleMethod)
        case _ => contextRdd.reproject(destCRS, layoutScheme, resampleMethod)
      }
    }

    tileLayerRdd.persist(cacheLevel)

    def buildPyramid(zoom: Int, rdd: TileLayerRDD[K]): List[(Int, TileLayerRDD[K])] = {
      if (zoom >= 1) {
        rdd.persist(cacheLevel)
        sink(rdd, zoom)
        val pyramidLevel @ (nextZoom, nextRdd) = Pyramid.up(rdd, layoutScheme, zoom, partitioner)
        pyramidLevel :: buildPyramid(nextZoom, nextRdd)
      } else {
        sink(rdd, zoom)
        List((zoom, rdd))
      }
    }

    if (pyramid) buildPyramid(zoom, tileLayerRdd).foreach { case (z, rdd) => rdd.unpersist(true) }
    else sink(tileLayerRdd, zoom)
  }
}
