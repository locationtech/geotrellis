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

import geotrellis.spark._
import geotrellis.spark.tiling._

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.proj4._

import monocle.SimpleLens
import monocle.syntax._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import org.apache.spark.broadcast.Broadcast

import scala.reflect.ClassTag

/** Represents the ingest process. 
  * An ingest process produces one or more layer from a set of input rasters.
  * 
  * The ingest process has the following steps:
  * 
  *  - Reproject tiles to the desired CRS:  (CRS, RDD[(Extent, CRS), Tile)]) -> RDD[(Extent, Tile)]
  *  - Determine the appropriate layer meta data for the layer. (CRS, LayoutScheme, RDD[(Extent, Tile)]) -> LayerMetaData)
  *  - Resample the rasters into the desired tile format. RDD[(Extent, Tile)] => RasterRDD[K]
  *  - Save the layer.
  *  - Pyramid the rasters to the most zoomed out level, and save those layers.
  * 
  * Ingesting is abstracted over the following variants:
  *  - The source of the input tiles, which are represented as an RDD of (T, Tile) tuples, where T: IngestKey
  *  - The LayoutScheme which will be used to determine how to retile the input tiles.
  *  - How the layer is saved.
  * 
  * Future improvements: Control the pyramid and `isUniform` behavior through configuration.
  */

abstract class Ingest[T: IngestKey, K: SpatialComponent: ClassTag](layoutScheme: LayoutScheme)(implicit tiler: Tiler[T, K]) extends Logging {
  def save(layerMetaData: LayerMetaData, rdd: RasterRDD[K]): Unit

  /** Override if you know the rasters are uniform in extent (performance optimization) */
  def isUniform = false

  // TODO: Make configurable.
  def pyramid(layerMetaData: LayerMetaData, rdd: RasterRDD[K]): Unit = {
    val layerId = layerMetaData.id
    logInfo(s"Saving RDD: ${layerId.name} for zoom level ${layerId.zoom}")
    save(layerMetaData, rdd)
    if (layerId.zoom > 1) Pyramid.up(rdd, layerMetaData.layoutLevel, layoutScheme)
  }

  def apply(sourceTiles: RDD[(T, Tile)], layerName: String, destCRS: CRS) = {
    val reprojectedTiles =
      sourceTiles
        .reproject(destCRS)

    val layerMetaData = 
      LayerMetaData.fromRdd(reprojectedTiles, layerName, destCRS, layoutScheme, isUniform) {  key: T => 
        key.projectedExtent.extent 
      }

    val rasterRdd = tiler.tile(reprojectedTiles, layerMetaData.rasterMetaData)

    pyramid(layerMetaData, rasterRdd)
  }
}
