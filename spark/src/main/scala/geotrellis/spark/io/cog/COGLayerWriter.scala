/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.io.index._

import spray.json._
import org.apache.spark.rdd.RDD

import scala.reflect._

trait COGLayerWriter {
  def writeCOGLayer[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag
  ](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]]
  ): Unit

  def write[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](
    layerName: String,
    tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    tileZoom: Int,
    keyIndexMethod: KeyIndexMethod[K]
  )(implicit tc: Iterable[(SpatialKey, V)] => GeoTiffSegmentConstructMethods[SpatialKey, V]): Unit =
    tiles.metadata.bounds match {
      case keyBounds: KeyBounds[K] =>
        val cogLayer = COGLayer(tiles, tileZoom)
        println(cogLayer.metadata.toJson.prettyPrint)
        val keyIndexes =
          cogLayer.metadata.zoomRangeInfos.
            map { case (zr, bounds) => zr -> keyIndexMethod.createIndex(bounds) }.
            toMap
        writeCOGLayer(layerName, cogLayer, keyIndexes)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }
}
