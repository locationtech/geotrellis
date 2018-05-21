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
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.spark._
import geotrellis.spark.pyramid._
import geotrellis.spark.reproject._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel
import scala.reflect.ClassTag

object MultibandIngest {
  def apply[T: ClassTag: ? => TilerKeyMethods[T, K]: Component[?, ProjectedExtent], K: SpatialComponent: Boundable: ClassTag](
    sourceTiles: RDD[(T, MultibandTile)],
    destCRS: CRS,
    layoutScheme: LayoutScheme,
    pyramid: Boolean = false,
    cacheLevel: StorageLevel = StorageLevel.NONE,
    resampleMethod: ResampleMethod = NearestNeighbor,
    partitioner: Option[Partitioner] = None,
    bufferSize: Option[Int] = None,
    maxZoom: Option[Int] = None,
    tileSize: Option[Int] = None)
    (sink: (MultibandTileLayerRDD[K], Int) => Unit): Unit = {

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

    def buildPyramid(zoom: Int, rdd: MultibandTileLayerRDD[K]): List[(Int, MultibandTileLayerRDD[K])] = {
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
